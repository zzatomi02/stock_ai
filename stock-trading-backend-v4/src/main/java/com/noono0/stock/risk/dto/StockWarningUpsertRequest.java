package com.noono0.stock.risk.dto;

import jakarta.validation.constraints.NotBlank;

public record StockWarningUpsertRequest(
        @NotBlank String stockCode,
        String stockName,
        @NotBlank String warningType,
        Integer riskScoreAdjustment,
        Boolean buyBlocked,
        String riskReason) {}
