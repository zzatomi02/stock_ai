package com.noono0.stock.tradingflow.service;

import com.noono0.stock.ai.platform.enums.AiBatchPhase;
import com.noono0.stock.ai.platform.service.AiAnalysisTargetCollector;
import com.noono0.stock.ai.platform.service.AiBatchOrchestratorService;
import com.noono0.stock.news.service.NewsIngestService;
import com.noono0.stock.tradingflow.config.TradingFlowProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 장전 흐름: 관심종목 준비 → 뉴스 수집 → AI 기업 분석 → DB 저장.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreMarketPipelineService {

    private final TradingFlowProperties flowProperties;
    private final WatchlistPreparationService watchlistPreparationService;
    private final NewsIngestService newsIngestService;
    private final AiAnalysisTargetCollector targetCollector;
    private final AiBatchOrchestratorService batchOrchestrator;

    public Map<String, Object> run() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!flowProperties.isPreMarketPipelineEnabled()) {
            result.put("skipped", true);
            return result;
        }

        int watchlist = watchlistPreparationService.prepareFromTodaySignals();
        result.put("watchlistPrepared", watchlist);

        int newsSaved = 0;
        if (flowProperties.isPreMarketNewsCollectEnabled()) {
            for (AiAnalysisTargetCollector.StockTarget t :
                    targetCollector.collect(AiBatchPhase.PRE_MARKET)) {
                String query =
                        (t.stockName() != null ? t.stockName() : t.stockCode())
                                + flowProperties.getPreMarketNewsQuerySuffix();
                newsSaved +=
                        newsIngestService.ingest(
                                query,
                                flowProperties.getPreMarketNewsDisplayPerStock(),
                                t.stockCode());
            }
        }
        result.put("newsArticlesSaved", newsSaved);

        int aiProcessed = batchOrchestrator.runPhase(AiBatchPhase.PRE_MARKET);
        result.put("aiJobsProcessed", aiProcessed);
        result.put("phase", "PRE_MARKET");

        log.info("[PRE-MARKET-PIPELINE] {}", result);
        return result;
    }
}
