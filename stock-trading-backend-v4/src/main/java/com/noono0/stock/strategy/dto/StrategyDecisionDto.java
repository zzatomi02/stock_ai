package com.noono0.stock.strategy.dto;

import com.noono0.stock.strategy.domain.enums.MarketCondition;
import com.noono0.stock.strategy.domain.enums.MarketTimeWindow;
import com.noono0.stock.strategy.domain.enums.RecommendedAction;
import com.noono0.stock.strategy.domain.enums.SignalType;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 단일 전략에 대한 8단계 판단 + 최종 점수.
 *
 * <ol>
 *   <li>지금 시장은 어떤 장인가?
 *   <li>지금은 몇 시인가?
 *   <li>이 시간대에 이 전략을 써도 되는가?
 *   <li>오늘 관리자가 이 전략을 껐는가?
 *   <li>시장 상태상 이 전략이 유리한가?
 *   <li>시간대상 이 전략이 유리한가?
 *   <li>점수와 수급이 기준을 넘었는가?
 *   <li>매수/매도 근거가 충분한가?
 * </ol>
 */
public record StrategyDecisionDto(
        String strategyType,
        String strategyName,
        MarketCondition marketCondition,
        MarketTimeWindow marketTimeWindow,
        boolean masterEnabled,
        boolean todayRuntimeAllows,
        boolean marketRuleAllows,
        boolean timeWindowAllows,
        boolean strategyExecutable,
        boolean scoresPassThreshold,
        boolean rationaleSufficient,
        boolean finalSignalAllowed,
        SignalType signalType,
        RecommendedAction recommendedAction,
        StrategyComponentScores componentScores,
        ScoreAdjustmentResult scoreAdjustment,
        double marketWeightMultiplier,
        double timeWeightMultiplier,
        double finalWeightMultiplier,
        Integer minScoreThreshold,
        String excludedReason,
        String decisionSummary) {

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("strategyType", strategyType);
        m.put("strategyName", strategyName);
        m.put("marketCondition", marketCondition != null ? marketCondition.name() : null);
        m.put("marketTimeWindow", marketTimeWindow != null ? marketTimeWindow.name() : null);
        m.put("checks", Map.of(
                "whatMarket", marketCondition != null ? marketCondition.label() : null,
                "whatTime", marketTimeWindow != null ? marketTimeWindow.label() : null,
                "allowedInTimeWindow", timeWindowAllows,
                "adminOffToday", !todayRuntimeAllows,
                "favorableInMarket", marketRuleAllows,
                "favorableInTimeWindow", timeWindowAllows,
                "scoresPass", scoresPassThreshold,
                "rationaleSufficient", rationaleSufficient));
        m.put("masterEnabled", masterEnabled);
        m.put("strategyExecutable", strategyExecutable);
        m.put("finalSignalAllowed", finalSignalAllowed);
        m.put("signalType", signalType != null ? signalType.name() : null);
        m.put("recommendedAction", recommendedAction != null ? recommendedAction.name() : null);
        if (componentScores != null) {
            m.put(
                    "scores",
                    Map.of(
                            "newsScore", componentScores.newsScore(),
                            "supplyScore", componentScores.supplyScore(),
                            "technicalScore", componentScores.technicalScore(),
                            "riskPenalty", componentScores.riskPenalty(),
                            "compositeRawScore", componentScores.compositeRawScore()));
        }
        m.put("marketWeightMultiplier", marketWeightMultiplier);
        m.put("timeWeightMultiplier", timeWeightMultiplier);
        m.put("finalWeightMultiplier", finalWeightMultiplier);
        if (scoreAdjustment != null) {
            m.put("adjustedFinalScore", scoreAdjustment.adjustedFinalScore());
            m.put("compositeRawScore", scoreAdjustment.compositeRawScore());
            m.put("scoreFormula", scoreAdjustment.formulaSummary());
        }
        m.put("minScoreThreshold", minScoreThreshold);
        m.put("excludedReason", excludedReason);
        m.put("decisionSummary", decisionSummary);
        return m;
    }
}
