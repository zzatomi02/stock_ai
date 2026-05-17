package com.noono0.stock.strategy.dto;

import com.noono0.stock.strategy.domain.StrategyMaster;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** 전략 분석 16단계 실행 결과 */
public record StrategyAnalysisResultDto(
        String strategyCode,
        String strategyLabel,
        boolean executed,
        String status,
        String side,
        String marketCondition,
        String marketTimeWindow,
        BigDecimal rawFinalScore,
        BigDecimal adjustedFinalScore,
        BigDecimal marketWeightMultiplier,
        BigDecimal timeWeightMultiplier,
        BigDecimal finalWeightMultiplier,
        int minScoreThreshold,
        String signalGrade,
        boolean passedMinScore,
        Long strategySignalId,
        String strategySelectedReason,
        String strategyExcludedReason,
        String reasonMessage,
        String executionLog) {

    public static StrategyAnalysisResultDto skipped(StrategyMaster master, StrategyDecisionDto decision) {
        return new StrategyAnalysisResultDto(
                master.getStrategyType(),
                master.getStrategyName(),
                false,
                "SKIPPED",
                null,
                decision.marketCondition() != null ? decision.marketCondition().name() : null,
                decision.marketTimeWindow() != null ? decision.marketTimeWindow().name() : null,
                null,
                null,
                java.math.BigDecimal.valueOf(decision.marketWeightMultiplier()),
                java.math.BigDecimal.valueOf(decision.timeWeightMultiplier()),
                java.math.BigDecimal.valueOf(decision.finalWeightMultiplier()),
                0,
                null,
                false,
                null,
                null,
                decision.excludedReason(),
                decision.excludedReason(),
                "비활성화된 전략 — 분석 생략 (policy #10)");
    }

    public static StrategyAnalysisResultDto skipped(StrategyMaster master, EffectiveStrategyDto effective) {
        return new StrategyAnalysisResultDto(
                master.getStrategyType(),
                master.getStrategyName(),
                false,
                "SKIPPED",
                null,
                effective.marketRegime(),
                effective.timeSlotCode(),
                null,
                null,
                java.math.BigDecimal.valueOf(effective.marketWeight()),
                java.math.BigDecimal.valueOf(effective.timeWeight()),
                java.math.BigDecimal.valueOf(effective.combinedWeight()),
                0,
                null,
                false,
                null,
                null,
                effective.reasonSummary(),
                effective.reasonSummary(),
                "비활성화된 전략 — 분석 생략 (policy #10)");
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("strategyCode", strategyCode);
        m.put("strategyLabel", strategyLabel);
        m.put("executed", executed);
        m.put("status", status);
        m.put("side", side);
        m.put("marketCondition", marketCondition);
        m.put("marketTimeWindow", marketTimeWindow);
        m.put("rawFinalScore", rawFinalScore);
        m.put("adjustedFinalScore", adjustedFinalScore);
        m.put("marketWeightMultiplier", marketWeightMultiplier);
        m.put("timeWeightMultiplier", timeWeightMultiplier);
        m.put("finalWeightMultiplier", finalWeightMultiplier);
        m.put("minScoreThreshold", minScoreThreshold);
        m.put("signalGrade", signalGrade);
        m.put("passedMinScore", passedMinScore);
        m.put("strategySignalId", strategySignalId);
        m.put("strategySelectedReason", strategySelectedReason);
        m.put("strategyExcludedReason", strategyExcludedReason);
        m.put("reasonMessage", reasonMessage);
        m.put("executionLog", executionLog);
        return m;
    }
}
