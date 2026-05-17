package com.noono0.stock.tradingflow.service;

import com.noono0.stock.news.domain.NewsArticle;
import com.noono0.stock.strategy.service.StrategyAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 장중 흐름: 뉴스/신호 발생 → Rule → Risk → AI 캐시 → strategy_signal 저장.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntradaySignalPipelineService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StrategyAnalysisService strategyAnalysisService;

    public void onNewsArticleSaved(NewsArticle article) {
        if (article.getId() == null) {
            return;
        }
        try {
            var results = strategyAnalysisService.analyzeArticle(article, LocalDateTime.now(KST));
            long candidates = results.stream().filter(r -> "CANDIDATE".equals(r.status())).count();
            log.info("[INTRADAY-PIPELINE] article={} candidates={}", article.getId(), candidates);
        } catch (Exception e) {
            log.warn("[INTRADAY-PIPELINE] article={} 실패 — {}", article.getId(), e.getMessage());
        }
    }
}
