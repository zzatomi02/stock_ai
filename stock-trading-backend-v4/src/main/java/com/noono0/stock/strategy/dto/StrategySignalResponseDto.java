package com.noono0.stock.strategy.dto;

import com.noono0.stock.strategy.domain.StrategySignal;
import com.noono0.stock.strategy.domain.enums.MarketCondition;
import com.noono0.stock.strategy.domain.enums.MarketTimeWindow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/** strategy_signal API 응답 (목록·상세) */
public record StrategySignalResponseDto(
        Long id,
        String strategyCode,
        Long newsArticleId,
        String stockCode,
        String stockName,
        String side,
        String tradeDate,
        String marketCondition,
        String marketConditionLabel,
        String marketTimeWindow,
        String marketTimeWindowLabel,
        BigDecimal rawFinalScore,
        BigDecimal adjustedFinalScore,
        BigDecimal marketWeightMultiplier,
        BigDecimal timeWeightMultiplier,
        BigDecimal finalWeightMultiplier,
        Integer minScoreThreshold,
        String signalGrade,
        String status,
        String strategySelectedReason,
        String strategyExcludedReason,
        String reasonMessage,
        String executionLog,
        Integer ruleBasedScore,
        BigDecimal aiOverallScore,
        BigDecimal aiBuyScore,
        BigDecimal aiRiskScore,
        BigDecimal aiBlendedScore,
        String aiDecision,
        String aiSummary,
        Long aiCompanyAnalysisId,
        Boolean riskCheckPassed,
        String riskBlockReason,
        String createdAt) {

    public static StrategySignalResponseDto from(StrategySignal s) {
        String mc = s.getMarketCondition() != null ? s.getMarketCondition() : s.getMarketRegime();
        String mw = s.getMarketTimeWindow() != null ? s.getMarketTimeWindow() : s.getTimeSlotCode();
        return new StrategySignalResponseDto(
                s.getId(),
                s.getStrategyCode(),
                s.getNewsArticleId(),
                s.getStockCode(),
                s.getStockName(),
                s.getSide(),
                s.getTradeDate() != null ? s.getTradeDate().toString() : null,
                mc,
                labelMarketCondition(mc),
                mw,
                labelTimeWindow(mw),
                scale2(s.getRawFinalScore()),
                scale2(s.getAdjustedFinalScore()),
                scale2(firstDecimal(s.getMarketWeightMultiplier(), s.getMarketWeight())),
                scale2(firstDecimal(s.getTimeWeightMultiplier(), s.getTimeWeight())),
                scale2(s.getFinalWeightMultiplier()),
                s.getMinScoreThreshold(),
                s.getSignalGrade(),
                s.getStatus(),
                s.getStrategySelectedReason(),
                s.getStrategyExcludedReason(),
                s.getReasonMessage(),
                s.getExecutionLog(),
                s.getRuleBasedScore(),
                scale2(s.getAiOverallScore()),
                scale2(s.getAiBuyScore()),
                scale2(s.getAiRiskScore()),
                scale2(s.getAiBlendedScore()),
                s.getAiDecision(),
                s.getAiSummary(),
                s.getAiCompanyAnalysisId(),
                s.getRiskCheckPassed(),
                s.getRiskBlockReason(),
                s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("strategyCode", strategyCode);
        m.put("newsArticleId", newsArticleId);
        m.put("stockCode", stockCode);
        m.put("stockName", stockName);
        m.put("side", side);
        m.put("tradeDate", tradeDate);
        m.put("marketCondition", marketCondition);
        m.put("marketConditionLabel", marketConditionLabel);
        m.put("marketTimeWindow", marketTimeWindow);
        m.put("marketTimeWindowLabel", marketTimeWindowLabel);
        m.put("rawFinalScore", rawFinalScore);
        m.put("adjustedFinalScore", adjustedFinalScore);
        m.put("marketWeightMultiplier", marketWeightMultiplier);
        m.put("timeWeightMultiplier", timeWeightMultiplier);
        m.put("finalWeightMultiplier", finalWeightMultiplier);
        m.put("minScoreThreshold", minScoreThreshold);
        m.put("signalGrade", signalGrade);
        m.put("status", status);
        m.put("strategySelectedReason", strategySelectedReason);
        m.put("strategyExcludedReason", strategyExcludedReason);
        m.put("reasonMessage", reasonMessage);
        m.put("executionLog", executionLog);
        m.put("ruleBasedScore", ruleBasedScore);
        m.put("aiOverallScore", aiOverallScore);
        m.put("aiBlendedScore", aiBlendedScore);
        m.put("aiDecision", aiDecision);
        m.put("riskCheckPassed", riskCheckPassed);
        m.put("riskBlockReason", riskBlockReason);
        m.put("createdAt", createdAt);
        return m;
    }

    /** 목록용 요약 */
    public Map<String, Object> toSummaryMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("strategyCode", strategyCode);
        m.put("stockCode", stockCode);
        m.put("stockName", stockName);
        m.put("side", side);
        m.put("adjustedFinalScore", adjustedFinalScore);
        m.put("signalGrade", signalGrade);
        m.put("status", status);
        m.put("marketConditionLabel", marketConditionLabel);
        m.put("marketTimeWindowLabel", marketTimeWindowLabel);
        return m;
    }

    private static BigDecimal scale2(BigDecimal v) {
        if (v == null) {
            return null;
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal firstDecimal(BigDecimal primary, Double legacy) {
        if (primary != null) {
            return primary;
        }
        if (legacy == null) {
            return null;
        }
        return BigDecimal.valueOf(legacy).setScale(2, RoundingMode.HALF_UP);
    }

    private static String labelMarketCondition(String code) {
        if (code == null) {
            return "";
        }
        try {
            return MarketCondition.valueOf(code).label();
        } catch (IllegalArgumentException e) {
            return code;
        }
    }

    private static String labelTimeWindow(String code) {
        if (code == null) {
            return "";
        }
        try {
            return MarketTimeWindow.valueOf(code).label();
        } catch (IllegalArgumentException e) {
            return code;
        }
    }
}
