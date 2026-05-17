package com.noono0.stock.strategy.dto;

import java.util.List;

public record EnabledNowResponse(
        String tradeDate,
        String currentTime,
        String marketCondition,
        String marketTimeWindow,
        List<EnabledStrategyItemDto> strategies) {}
