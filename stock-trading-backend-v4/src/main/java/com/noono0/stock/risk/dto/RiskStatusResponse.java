package com.noono0.stock.risk.dto;

import com.noono0.stock.risk.config.RiskProperties;

import java.math.BigDecimal;

public record RiskStatusResponse(
        boolean buyHalted,
        boolean globalHalt,
        String haltReason,
        int buyOrderCountToday,
        int sellOrderCountToday,
        BigDecimal realizedPnlToday,
        double dailyLossRatePercent,
        double dailyMaxLossRatePercent,
        int dailyMaxBuyCount,
        boolean apiHealthy,
        long quoteDelayMs,
        RiskProperties config) {}
