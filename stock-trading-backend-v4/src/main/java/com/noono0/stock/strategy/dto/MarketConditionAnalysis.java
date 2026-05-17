package com.noono0.stock.strategy.dto;

import com.noono0.stock.strategy.domain.enums.MarketCondition;

/** 시장 상태 분석 결과 */
public record MarketConditionAnalysis(
        int marketScore,
        MarketCondition condition,
        String conditionLabel,
        String detectionSource,
        String reasonSummary,
        boolean themeConcentration,
        boolean largeCapConcentration,
        boolean smallCapConcentration,
        boolean highVolatility) {

    public static MarketConditionAnalysis ofScore(MarketCondition condition, int score, String reason) {
        return new MarketConditionAnalysis(
                score,
                condition,
                condition.label(),
                "MARKET_SCORE",
                reason,
                false,
                false,
                false,
                false);
    }
}
