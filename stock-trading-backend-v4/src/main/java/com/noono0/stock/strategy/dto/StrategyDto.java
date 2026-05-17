package com.noono0.stock.strategy.dto;

import com.noono0.stock.strategy.domain.Strategy;

import java.time.LocalDateTime;

/** API 응답용 (엔티티 직접 노출 방지) */
public record StrategyDto(
        Long id,
        String name,
        Boolean enabled,
        String targetMarket,
        Double riskLimitRate,
        Double takeProfitRate,
        Long maxBuyAmount,
        Boolean keywordScoreEnabled,
        Boolean aiScoreEnabled,
        Boolean autoTradeEnabled,
        Integer reentryCooldownMinutes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static StrategyDto from(Strategy s) {
        return new StrategyDto(
                s.getId(),
                s.getName(),
                s.getEnabled(),
                s.getTargetMarket(),
                s.getRiskLimitRate(),
                s.getTakeProfitRate(),
                s.getMaxBuyAmount(),
                s.getKeywordScoreEnabled(),
                s.getAiScoreEnabled(),
                s.getAutoTradeEnabled(),
                s.getReentryCooldownMinutes(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
