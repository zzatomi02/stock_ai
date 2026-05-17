package com.noono0.stock.ai.platform.service;

import com.noono0.stock.ai.platform.enums.AiExecutionTiming;
import com.noono0.stock.strategy.service.TradingClockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AiExecutionPolicyServiceTest {

    private final TradingClockService clock = new TradingClockService();
    private final AiExecutionPolicyService policy = new AiExecutionPolicyService(clock);

    @AfterEach
    void tearDown() {
        clock.clearOverride();
    }

    @Test
    void intraday_asyncBatch_allowed_butFastPathBlocked() {
        clock.runAt(
                LocalDateTime.of(2026, 5, 17, 10, 30),
                () -> {
                    assertEquals(AiExecutionTiming.INTRADAY_ASYNC, policy.resolveCurrentTiming());
                    assertTrue(policy.isLiveApiCallAllowed(AiExecutionTiming.INTRADAY_ASYNC));
                    assertThrows(IllegalStateException.class, () -> policy.assertFastPathNoLiveApi());
                });
    }

    @Test
    void intraday_blockedTiming_neverCallsApi() {
        assertFalse(policy.isLiveApiCallAllowed(AiExecutionTiming.INTRADAY_BLOCKED));
        assertThrows(
                IllegalStateException.class,
                () -> policy.assertLiveApiAllowed(AiExecutionTiming.INTRADAY_BLOCKED));
    }

    @Test
    void preMarket_allowsBatch() {
        clock.runAt(
                LocalDateTime.of(2026, 5, 17, 8, 20),
                () -> assertTrue(policy.isLiveApiCallAllowed(AiExecutionTiming.PRE_MARKET)));
    }
}
