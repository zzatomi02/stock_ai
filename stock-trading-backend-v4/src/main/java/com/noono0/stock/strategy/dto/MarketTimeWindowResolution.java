package com.noono0.stock.strategy.dto;

import com.noono0.stock.strategy.domain.enums.MarketTimeWindow;

import java.time.LocalTime;

public record MarketTimeWindowResolution(LocalTime evaluatedTime, MarketTimeWindow window, String label) {}
