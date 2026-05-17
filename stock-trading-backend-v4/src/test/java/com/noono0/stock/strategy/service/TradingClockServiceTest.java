package com.noono0.stock.strategy.service;

import com.noono0.stock.strategy.domain.enums.MarketTimeWindow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TradingClockServiceTest {

    private final TradingClockService clock = new TradingClockService();

    @AfterEach
    void tearDown() {
        clock.clearOverride();
    }

    @Test
    void runAt_fixesEvaluationTime_forTimeWindow() {
        LocalDateTime at = LocalDateTime.of(2026, 5, 17, 9, 1);
        clock.runAt(
                at,
                () -> {
                    assertEquals(LocalTime.of(9, 1), clock.currentTime());
                    assertEquals(
                            MarketTimeWindow.OPENING_NO_TRADE,
                            MarketTimeWindow.current(clock.currentTime()));
                });
    }

    @Test
    void runAt_at1515_resolvesClosingBet() {
        LocalDateTime at = LocalDateTime.of(2026, 5, 17, 15, 15);
        clock.runAt(
                at,
                () ->
                        assertEquals(
                                MarketTimeWindow.CLOSING_BET,
                                MarketTimeWindow.current(clock.currentTime())));
    }
}
