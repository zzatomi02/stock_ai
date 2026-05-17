package com.noono0.stock.strategy.dto;

import com.noono0.stock.strategy.domain.MarketConditionStrategy;

import java.math.BigDecimal;

public record MarketConditionStrategyDto(
        Long id,
        String marketCondition,
        String strategyType,
        boolean isEnabled,
        BigDecimal weightMultiplier,
        BigDecimal minScoreOverride,
        String description) {

    public static MarketConditionStrategyDto from(MarketConditionStrategy e) {
        return new MarketConditionStrategyDto(
                e.getId(),
                e.getMarketCondition(),
                e.getStrategyType(),
                Boolean.TRUE.equals(e.getEnabled()),
                e.getWeightMultiplier(),
                e.getMinScoreOverride(),
                e.getDescription());
    }
}
