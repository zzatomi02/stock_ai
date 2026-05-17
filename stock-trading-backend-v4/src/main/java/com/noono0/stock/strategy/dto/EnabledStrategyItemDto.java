package com.noono0.stock.strategy.dto;

import java.math.BigDecimal;

public record EnabledStrategyItemDto(
        String strategyType,
        boolean enabled,
        BigDecimal marketWeightMultiplier,
        BigDecimal timeWeightMultiplier,
        BigDecimal finalWeightMultiplier,
        Integer minScore,
        String selectedReason,
        String excludedReason) {

    public static EnabledStrategyItemDto enabled(
            String strategyType,
            double marketW,
            double timeW,
            double finalW,
            Integer minScore,
            String selectedReason) {
        return new EnabledStrategyItemDto(
                strategyType,
                true,
                bd(marketW),
                bd(timeW),
                bd(finalW),
                minScore,
                selectedReason,
                null);
    }

    public static EnabledStrategyItemDto disabled(String strategyType, String excludedReason) {
        return new EnabledStrategyItemDto(
                strategyType, false, null, null, null, null, null, excludedReason);
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
