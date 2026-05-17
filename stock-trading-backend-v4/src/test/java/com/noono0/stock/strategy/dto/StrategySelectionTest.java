package com.noono0.stock.strategy.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StrategySelectionTest {

    @Test
    void executable_requiresAllFourGates() {
        StrategySelection ok =
                StrategySelection.of("BREAKOUT", true, false, true, true, true, 1.0, 1.0);
        assertTrue(ok.executable());
        assertTrue(ok.failedChecks().isEmpty());

        StrategySelection masterOff =
                StrategySelection.of("BREAKOUT", false, false, true, true, true, 1.0, 1.0);
        assertFalse(masterOff.executable());
        assertTrue(masterOff.failedChecks().contains("strategy_master"));

        StrategySelection todayOff =
                StrategySelection.of("BREAKOUT", true, true, false, true, true, 1.0, 1.0);
        assertFalse(todayOff.executable());
        assertTrue(todayOff.failedChecks().contains("strategy_runtime_setting"));

        StrategySelection marketOff =
                StrategySelection.of("BREAKOUT", true, false, true, false, true, 1.0, 1.0);
        assertFalse(marketOff.executable());
        assertTrue(marketOff.failedChecks().contains("market_condition_strategy"));

        StrategySelection timeOff =
                StrategySelection.of("BREAKOUT", true, false, true, true, false, 1.0, 1.0);
        assertFalse(timeOff.executable());
        assertTrue(timeOff.failedChecks().contains("strategy_time_window"));
    }
}
