package com.noono0.stock.strategy.dto;

import java.util.Map;

/** 4중 조합(기본·오늘·시장·시간) 적용 후 최종 실행 상태 */
public record EffectiveStrategyDto(
        String code,
        String label,
        String category,
        boolean buyStrategy,
        boolean baseEnabled,
        Boolean todayOverride,
        boolean todayEnabled,
        String marketRegime,
        boolean marketRuleEnabled,
        double marketWeight,
        String timeSlotCode,
        String timeSlotLabel,
        boolean timeRuleEnabled,
        double timeWeight,
        boolean blockBuyInSlot,
        double combinedWeight,
        boolean effectiveEnabled,
        boolean allowBuy,
        String reasonSummary) {

    public Map<String, Object> toMap() {
        return Map.ofEntries(
                Map.entry("code", code),
                Map.entry("strategyType", code),
                Map.entry("label", label),
                Map.entry("category", category),
                Map.entry("buyStrategy", buyStrategy),
                Map.entry("baseEnabled", baseEnabled),
                Map.entry("todayOverride", todayOverride != null),
                Map.entry("todayEnabled", todayEnabled),
                Map.entry("marketCondition", marketRegime),
                Map.entry("marketRegime", marketRegime),
                Map.entry("marketRuleEnabled", marketRuleEnabled),
                Map.entry("marketWeight", marketWeight),
                Map.entry("marketTimeWindow", timeSlotCode != null ? timeSlotCode : ""),
                Map.entry("timeSlotCode", timeSlotCode != null ? timeSlotCode : ""),
                Map.entry("timeSlotLabel", timeSlotLabel != null ? timeSlotLabel : ""),
                Map.entry("timeRuleEnabled", timeRuleEnabled),
                Map.entry("timeWeight", timeWeight),
                Map.entry("blockBuyInSlot", blockBuyInSlot),
                Map.entry("combinedWeight", combinedWeight),
                Map.entry("effectiveEnabled", effectiveEnabled),
                Map.entry("allowBuy", allowBuy),
                Map.entry("reasonSummary", reasonSummary));
    }
}
