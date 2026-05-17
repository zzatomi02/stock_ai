package com.noono0.stock.strategy.dto;

import jakarta.validation.constraints.NotNull;

public record StrategyEnabledUpdateRequest(@NotNull Boolean isEnabled) {}
