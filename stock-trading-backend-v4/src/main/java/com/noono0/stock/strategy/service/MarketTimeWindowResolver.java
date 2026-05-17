package com.noono0.stock.strategy.service;

import com.noono0.stock.strategy.domain.enums.MarketTimeWindow;
import com.noono0.stock.strategy.dto.MarketTimeWindowResolution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketTimeWindowResolver {

    private final TradingClockService tradingClock;

    public MarketTimeWindowResolution resolve() {
        return resolve(tradingClock.currentTime());
    }

    public MarketTimeWindowResolution resolve(LocalTime time) {
        MarketTimeWindow window = MarketTimeWindow.current(time);
        log.debug("[TIME-WINDOW] {} → {} ({}~{})", time, window.name(), window.start(), window.end());
        return new MarketTimeWindowResolution(time, window, window.label());
    }
}
