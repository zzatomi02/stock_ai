package com.noono0.stock.signal.service;

import com.noono0.stock.market.service.MarketMoodService;
import com.noono0.stock.news.domain.NewsArticle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignalScoringService {
    private final MarketMoodService marketMoodService;

    public record ScoreBreakdown(
            int finalScore,
            int aiComponent,
            int keywordComponent,
            int marketComponent,
            int volumeComponent,
            int riskPenalty,
            String grade) {}

    public ScoreBreakdown compute(NewsArticle article, String side) {
        int ai = article.getAiScore() != null ? article.getAiScore() : 50;
        int keyword = article.getKeywordScore() != null ? article.getKeywordScore() : 50;
        int market = marketMoodService.currentScore();
        int volume = 50;
        int riskPenalty = estimateRiskPenalty(article);

        double raw =
                ai * 0.50 + keyword * 0.20 + market * 0.15 + volume * 0.10 - riskPenalty;
        int finalScore = (int) Math.round(Math.max(0, Math.min(100, raw)));

        double conf = article.getLlmConfidence() != null ? article.getLlmConfidence() : 0.5;
        String grade = grade(finalScore, conf, market, article, side);

        return new ScoreBreakdown(finalScore, ai, keyword, market, volume, riskPenalty, grade);
    }

    private int estimateRiskPenalty(NewsArticle article) {
        if (article.getLlmRiskJson() == null || article.getLlmRiskJson().length() < 3) {
            return 0;
        }
        int len = article.getLlmRiskJson().split(",").length;
        return Math.min(30, len * 5);
    }

    private String grade(int finalScore, double confidence, int market, NewsArticle article, String side) {
        boolean riskHeavy = article.getLlmRiskJson() != null && article.getLlmRiskJson().length() > 80;
        if (finalScore >= 90 && confidence >= 0.8 && market >= 56 && !riskHeavy) {
            return "S";
        }
        if (finalScore >= 80 && confidence >= 0.75) {
            return "A";
        }
        if (finalScore >= 70) {
            return "B";
        }
        if (finalScore >= 55) {
            return "C";
        }
        return "D";
    }

    public boolean passesBuyRules(NewsArticle article, ScoreBreakdown score) {
        boolean sentimentOk =
                "POSITIVE".equalsIgnoreCase(article.getLlmSentiment())
                        || article.getLlmSentiment() == null && score.keywordComponent() >= 62;
        if (!sentimentOk && score.aiComponent() < 65) {
            return false;
        }
        if (article.getLlmConfidence() != null && article.getLlmConfidence() < 0.7 && score.aiComponent() >= 50) {
            return false;
        }
        if (score.finalScore() < 75 && article.getLlmSentiment() != null) {
            return false;
        }
        if (score.finalScore() < 62 && article.getLlmSentiment() == null) {
            return false;
        }
        if (marketMoodService.currentScore() <= 30) {
            return false;
        }
        return !"D".equals(score.grade());
    }

    public boolean passesSellRules(NewsArticle article, ScoreBreakdown score) {
        if (!"NEGATIVE".equalsIgnoreCase(article.getLlmSentiment()) && score.keywordComponent() > 45) {
            return false;
        }
        if (article.getLlmConfidence() != null && article.getLlmConfidence() < 0.7) {
            return false;
        }
        return score.finalScore() <= 45 || "NEGATIVE".equalsIgnoreCase(article.getLlmSentiment());
    }
}
