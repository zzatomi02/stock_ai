package com.noono0.stock.strategy.dto;

import java.util.ArrayList;
import java.util.List;

/** 4중 ON/OFF 게이트 검사 결과 */
public record StrategySelection(
        String strategyType,
        boolean masterEnabled,
        boolean hasRuntimeOverride,
        boolean runtimeEnabled,
        boolean todayRuntimeEnabled,
        boolean marketRuleEnabled,
        boolean timeWindowEnabled,
        boolean executable,
        double marketWeightMultiplier,
        double timeWeightMultiplier,
        double combinedWeightMultiplier,
        List<String> failedChecks) {

    public static StrategySelection of(
            String strategyType,
            boolean masterEnabled,
            boolean hasRuntimeOverride,
            boolean runtimeEnabled,
            boolean marketRuleEnabled,
            boolean timeWindowEnabled,
            double marketWeight,
            double timeWeight) {
        boolean todayRuntimeEnabled = !hasRuntimeOverride || runtimeEnabled;
        List<String> failed = new ArrayList<>();
        if (!masterEnabled) {
            failed.add("strategy_master");
        }
        if (!todayRuntimeEnabled) {
            failed.add("strategy_runtime_setting");
        }
        if (!marketRuleEnabled) {
            failed.add("market_condition_strategy");
        }
        if (!timeWindowEnabled) {
            failed.add("strategy_time_window");
        }
        boolean executable =
                masterEnabled && todayRuntimeEnabled && marketRuleEnabled && timeWindowEnabled;
        double combined = round2(marketWeight * timeWeight);
        return new StrategySelection(
                strategyType,
                masterEnabled,
                hasRuntimeOverride,
                runtimeEnabled,
                todayRuntimeEnabled,
                marketRuleEnabled,
                timeWindowEnabled,
                executable,
                marketWeight,
                timeWeight,
                combined,
                List.copyOf(failed));
    }

    public String reasonSummary() {
        if (executable) {
            return "4중 ON/OFF 통과 (기본·오늘·시장·시간)";
        }
        return "실행 불가 — 미통과: " + String.join(", ", failedChecks);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
