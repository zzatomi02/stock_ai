package com.noono0.stock.strategy.dto;

import com.noono0.stock.strategy.domain.StrategyTimeWindowRule;

import java.math.BigDecimal;
import java.time.LocalTime;

public record StrategyTimeWindowDto(
        Long id,
        String strategyType,
        String marketTimeWindow,
        String windowName,
        LocalTime startTime,
        LocalTime endTime,
        boolean isEnabled,
        BigDecimal weightMultiplier,
        BigDecimal minScoreOverride,
        Integer maxTradeCount,
        String description) {

    public static StrategyTimeWindowDto from(StrategyTimeWindowRule e) {
        return new StrategyTimeWindowDto(
                e.getId(),
                e.getStrategyType(),
                e.getMarketTimeWindow(),
                e.getWindowName(),
                e.getStartTime(),
                e.getEndTime(),
                Boolean.TRUE.equals(e.getEnabled()),
                e.getWeightMultiplier(),
                e.getMinScoreOverride(),
                e.getMaxTradeCount(),
                e.getDescription());
    }
}
