package com.noono0.stock.strategy.service;

import com.noono0.stock.ai.platform.service.AiScoreBlendService;
import com.noono0.stock.ai.platform.service.CachedAiAnalysisQueryService;
import com.noono0.stock.market.service.MarketMoodService;
import com.noono0.stock.news.domain.NewsArticle;
import com.noono0.stock.strategy.dto.StrategyComponentScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 뉴스·수급·기술·리스크 구성 점수 산출.
 *
 * <p>장중에는 {@link CachedAiAnalysisQueryService} DB 캐시만 사용(외부 AI API 호출 없음).
 */
@Service
@RequiredArgsConstructor
public class StrategyComponentScorer {

    private final MarketMoodService marketMoodService;
    private final CachedAiAnalysisQueryService cachedAiQuery;
    private final AiScoreBlendService aiScoreBlendService;

    public StrategyComponentScores score(NewsArticle article, String side) {
        int news = newsScore(article);
        int supply = supplyScore(article);
        int technical = technicalScore(article);
        int riskPenalty = riskPenalty(article);

        int ruleBased =
                (int)
                        Math.round(
                                news * 0.35
                                        + supply * 0.25
                                        + technical * 0.25
                                        + marketMoodService.currentScore() * 0.15);

        int composite = ruleBased;
        if (StringUtils.hasText(article.getStockCode()) && "BUY".equalsIgnoreCase(side)) {
            var blend = aiScoreBlendService.blend(ruleBased, article.getStockCode(), null);
            composite = blend.finalBuyScore();
            riskPenalty = Math.max(riskPenalty, blend.riskPenalty());
        } else {
            if (StringUtils.hasText(article.getStockCode())) {
                var cached = cachedAiQuery.getBestForTrading(article.getStockCode());
                if (cached.isPresent() && cached.get().riskScore() != null) {
                    riskPenalty = Math.max(riskPenalty, cached.get().riskScore().intValue() / 3);
                }
            }
            composite =
                    (int)
                            Math.round(
                                    Math.max(
                                            0,
                                            Math.min(
                                                    100,
                                                    ruleBased - riskPenalty)));
        }
        String grade = grade(composite);
        return StrategyComponentScores.of(news, supply, technical, riskPenalty, composite, grade);
    }

    private static int newsScore(NewsArticle article) {
        int ai = article.getAiScore() != null ? article.getAiScore() : 50;
        int keyword = article.getKeywordScore() != null ? article.getKeywordScore() : 50;
        return (int) Math.round(ai * 0.7 + keyword * 0.3);
    }

    /** 더미: AI·감성 기반 수급 추정 */
    private static int supplyScore(NewsArticle article) {
        if ("POSITIVE".equalsIgnoreCase(article.getLlmSentiment())) {
            return 72;
        }
        if ("NEGATIVE".equalsIgnoreCase(article.getLlmSentiment())) {
            return 35;
        }
        return 55;
    }

    /** 더미: 키워드·AI 기반 기술 추정 */
    private static int technicalScore(NewsArticle article) {
        int ai = article.getAiScore() != null ? article.getAiScore() : 50;
        return Math.min(100, Math.max(0, ai + 5));
    }

    private static int riskPenalty(NewsArticle article) {
        if (article.getLlmRiskJson() == null || article.getLlmRiskJson().length() < 3) {
            return 0;
        }
        return Math.min(30, article.getLlmRiskJson().split(",").length * 5);
    }

    private static String grade(int score) {
        if (score >= 90) return "S";
        if (score >= 80) return "A";
        if (score >= 70) return "B";
        if (score >= 55) return "C";
        return "D";
    }
}
