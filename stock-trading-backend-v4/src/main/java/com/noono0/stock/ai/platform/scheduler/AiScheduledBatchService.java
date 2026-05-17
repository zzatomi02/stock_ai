package com.noono0.stock.ai.platform.scheduler;

import com.noono0.stock.ai.platform.config.AiPlatformProperties;
import com.noono0.stock.ai.platform.enums.AiBatchPhase;
import com.noono0.stock.ai.platform.service.AiBatchOrchestratorService;
import com.noono0.stock.ai.platform.service.AiExecutionPolicyService;
import com.noono0.stock.tradingflow.config.TradingFlowProperties;
import com.noono0.stock.tradingflow.service.PreMarketPipelineService;
import com.noono0.stock.tradingflow.service.PostMarketPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 4단계 AI 배치 스케줄.
 *
 * <ul>
 *   <li>08:10~08:55 장전 — 10분
 *   <li>09:00~15:30 장중 비동기 — 3~5분
 *   <li>14:30~15:10 종가 후보 — 5분
 *   <li>15:40+ 장마감 — 15분
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiScheduledBatchService {

    private final AiPlatformProperties properties;
    private final AiExecutionPolicyService executionPolicy;
    private final AiBatchOrchestratorService batchOrchestrator;
    private final TradingFlowProperties flowProperties;
    private final PreMarketPipelineService preMarketPipelineService;
    private final PostMarketPipelineService postMarketPipelineService;

    @Scheduled(cron = "${app.ai.platform.cron.pre-market:0 10/10 8 * * MON-FRI}")
    public void preMarketBatch() {
        if (!properties.isBatchEnabled()) return;
        if (!executionPolicy.isPreMarketWindow()) return;
        if (flowProperties.isPreMarketPipelineEnabled()) {
            preMarketPipelineService.run();
        } else {
            batchOrchestrator.runPhase(AiBatchPhase.PRE_MARKET);
        }
    }

    @Scheduled(fixedDelayString = "${app.ai.platform.intraday-interval-ms:180000}")
    public void intradayAsyncBatch() {
        if (!properties.isBatchEnabled()) return;
        if (executionPolicy.isIntradayAsyncWindow()) {
            batchOrchestrator.runPhase(AiBatchPhase.INTRADAY_ASYNC);
        }
    }

    @Scheduled(fixedDelayString = "${app.ai.platform.closing-interval-ms:300000}")
    public void closingCandidateBatch() {
        if (!properties.isBatchEnabled()) return;
        if (executionPolicy.isClosingWindow()) {
            batchOrchestrator.runPhase(AiBatchPhase.CLOSING_CANDIDATE);
        }
    }

    @Scheduled(cron = "${app.ai.platform.cron.post-market:0 40/15 15-23 * * MON-FRI}")
    public void postMarketBatch() {
        if (!properties.isBatchEnabled()) return;
        if (!executionPolicy.isPostMarketWindow()) return;
        if (flowProperties.isPostMarketPipelineEnabled()) {
            postMarketPipelineService.run();
        } else {
            batchOrchestrator.runPhase(AiBatchPhase.POST_MARKET);
        }
    }
}
