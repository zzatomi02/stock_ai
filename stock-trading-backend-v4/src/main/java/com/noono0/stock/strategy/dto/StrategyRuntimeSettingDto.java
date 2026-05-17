package com.noono0.stock.strategy.dto;

import com.noono0.stock.strategy.domain.StrategyRuntimeSetting;

public record StrategyRuntimeSettingDto(
        Long id,
        String tradeDate,
        String strategyType,
        boolean isEnabled,
        String reason,
        String updatedBy) {

    public static StrategyRuntimeSettingDto from(StrategyRuntimeSetting e) {
        return new StrategyRuntimeSettingDto(
                e.getId(),
                e.getTradeDate() != null ? e.getTradeDate().toString() : null,
                e.getStrategyType(),
                Boolean.TRUE.equals(e.getEnabled()),
                e.getReason(),
                e.getUpdatedBy());
    }
}
