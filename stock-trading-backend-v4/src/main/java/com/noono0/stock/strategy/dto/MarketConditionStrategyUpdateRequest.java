package com.noono0.stock.strategy.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MarketConditionStrategyUpdateRequest(
        @NotNull Boolean isEnabled,
        @NotNull BigDecimal weightMultiplier,
        BigDecimal minScoreOverride,
        String description) {}
