package com.noono0.stock.strategy.dto;

import com.noono0.stock.strategy.domain.StrategyMaster;

public record StrategySettingDto(
        String strategyType,
        String strategyName,
        String description,
        boolean isEnabled,
        int sortOrder) {

    public static StrategySettingDto from(StrategyMaster m) {
        return new StrategySettingDto(
                m.getStrategyType(),
                m.getStrategyName(),
                m.getDescription(),
                Boolean.TRUE.equals(m.getEnabled()),
                m.getSortOrder() != null ? m.getSortOrder() : 0);
    }
}
