package com.noono0.stock.strategy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StrategyRuntimeSettingRequest(
        @NotNull LocalDate tradeDate,
        @NotBlank String strategyType,
        @NotNull Boolean isEnabled,
        String reason,
        String updatedBy) {}
