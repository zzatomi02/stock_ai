package com.noono0.stock.tradingflow.service;

import com.noono0.stock.ai.platform.enums.AiBatchPhase;
import com.noono0.stock.ai.platform.service.AiBatchOrchestratorService;
import com.noono0.stock.tradingflow.config.TradingFlowProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 장마감 흐름: AI 심층 분석 → 매매 복기 → 전략 개선 추천 → 익일 관심종목 준비.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostMarketPipelineService {

    private final TradingFlowProperties flowProperties;
    private final AiBatchOrchestratorService batchOrchestrator;
    private final AiTradeReviewService tradeReviewService;
    private final StrategyImprovementService strategyImprovementService;
    private final WatchlistPreparationService watchlistPreparationService;

    public Map<String, Object> run() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!flowProperties.isPostMarketPipelineEnabled()) {
            result.put("skipped", true);
            return result;
        }

        int aiProcessed = batchOrchestrator.runPhase(AiBatchPhase.POST_MARKET);
        result.put("aiJobsProcessed", aiProcessed);

        int reviews = tradeReviewService.reviewTodayCandidates();
        result.put("tradeReviewsCreated", reviews);

        int recommendations = strategyImprovementService.generateFromToday();
        result.put("strategyRecommendations", recommendations);

        int watchlist = watchlistPreparationService.prepareNextDayFromPostMarket();
        result.put("nextDayWatchlistPrepared", watchlist);
        result.put("phase", "POST_MARKET");

        log.info("[POST-MARKET-PIPELINE] {}", result);
        return result;
    }
}
