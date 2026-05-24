package com.noono0.stock.tradingflow.service;

import com.noono0.stock.news.domain.NewsArticle;
import com.noono0.stock.signal.service.SignalEngine;
import com.noono0.stock.tradingflow.config.TradingFlowProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 뉴스 저장 후 단일 진입점 — strategy_signal(권장) 또는 legacy trading_signal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsArticleIngestPipeline {

    private final TradingFlowProperties flowProperties;
    private final IntradaySignalPipelineService intradaySignalPipeline;
    private final SignalEngine signalEngine;

    public void onArticleSaved(NewsArticle article) {
        if (article == null || article.getId() == null) {
            return;
        }
        if (flowProperties.isIngestTriggersStrategyAnalysis()) {
            intradaySignalPipeline.onNewsArticleSaved(article);
            return;
        }
        if (flowProperties.isLegacyTradingSignalOnIngest()) {
            try {
                signalEngine.generateFromArticle(article, SignalEngine.SYSTEM_USER);
            } catch (Exception exception) {
                log.warn("[INGEST-PIPELINE] legacy trading_signal 실패 article={} — {}", article.getId(), exception.getMessage());
            }
        }
    }
}
