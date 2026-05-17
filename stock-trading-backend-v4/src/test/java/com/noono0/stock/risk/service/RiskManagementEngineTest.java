package com.noono0.stock.risk.service;

import com.noono0.stock.broker.mapper.BrokerOrderAttemptMapper;
import com.noono0.stock.integration.kis.service.KisApiHealthService;
import com.noono0.stock.market.service.MarketMoodService;
import com.noono0.stock.risk.config.RiskProperties;
import com.noono0.stock.risk.domain.RiskDailyState;
import com.noono0.stock.risk.dto.RiskCheckResult;
import com.noono0.stock.risk.dto.WarningFilterResult;
import com.noono0.stock.strategy.service.TradingClockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RiskManagementEngineTest {

    @Mock RiskProperties properties;
    @Mock RiskDailyStateService dailyStateService;
    @Mock RiskStrategyDailyService strategyDailyService;
    @Mock RiskEventService riskEventService;
    @Mock MarketWarningFilter marketWarningFilter;
    @Mock MarketMoodService marketMoodService;
    @Mock KisApiHealthService apiHealthService;
    @Mock BrokerOrderAttemptMapper orderAttemptMapper;
    @Mock TradingClockService tradingClock;

    @InjectMocks RiskManagementEngine engine;

    @BeforeEach
    void setUp() {
        RiskProperties.TimeWindowTradeLimit w = new RiskProperties.TimeWindowTradeLimit();
        w.setStart("09:00");
        w.setEnd("09:10");
        w.setMaxTrades(3);
        when(properties.getTimeWindowLimits()).thenReturn(List.of(w));
        when(properties.getDailyMaxBuyCount()).thenReturn(50);
        when(properties.getSingleOrderMaxAmount()).thenReturn(10_000_000L);
        when(properties.getMaxPositionPerStrategyAmount()).thenReturn(2_000_000L);
        when(properties.getMaxPositionPerSymbolPercent()).thenReturn(10.0);
        when(properties.getReferenceEquity()).thenReturn(10_000_000L);
        when(properties.maxPositionPerSymbolAmount()).thenReturn(BigDecimal.valueOf(1_000_000));
        when(properties.getReentryCooldownMinutes()).thenReturn(30);
        when(properties.getMarketCrashMoodThreshold()).thenReturn(20);
        when(properties.isHaltOnApiError()).thenReturn(true);
        when(properties.getQuoteDelayMaxMs()).thenReturn(5000L);

        RiskDailyState daily = new RiskDailyState();
        daily.setBuyHalted(false);
        daily.setGlobalHalt(false);
        daily.setBuyOrderCount(0);
        when(dailyStateService.getOrCreateToday(any())).thenReturn(daily);

        when(apiHealthService.isApiHealthy()).thenReturn(true);
        when(apiHealthService.quoteDelayMs()).thenReturn(100L);
        when(marketMoodService.currentScore()).thenReturn(55);
        when(marketWarningFilter.evaluate(anyString())).thenReturn(WarningFilterResult.clear());
        when(strategyDailyService.isDisabledToday(any())).thenReturn(false);
        when(tradingClock.currentTime()).thenReturn(LocalTime.of(9, 5));
        when(tradingClock.today()).thenReturn(java.time.LocalDate.now());
        when(tradingClock.now()).thenReturn(java.time.LocalDateTime.now());
        when(orderAttemptMapper.countBySideAndCreatedAtBetween(any(), any(), any())).thenReturn(3);
    }

    @Test
    void blocksBuy_whenTimeWindowLimitReached() {
        RiskCheckResult r =
                engine.validateOrder(
                        new RiskManagementEngine.OrderRiskContext(
                                "u1", "005930", "BUY", "OPENING_BET", 500_000, 10));
        assertFalse(r.passed());
        assertTrue(r.failReason().contains("09:00"));
    }

    @Test
    void blocksBuy_whenWarningStock() {
        when(orderAttemptMapper.countBySideAndCreatedAtBetween(any(), any(), any())).thenReturn(0);
        when(marketWarningFilter.evaluate("000000"))
                .thenReturn(
                        WarningFilterResult.blocked(
                                "TRADING_HALT", "거래정지", 100));
        RiskCheckResult r =
                engine.validateOrder(
                        new RiskManagementEngine.OrderRiskContext(
                                "u1", "000000", "BUY", null, 100_000, 2));
        assertFalse(r.passed());
        assertTrue(r.failReason().contains("시장경보"));
    }

    @Test
    void blocksBuy_whenDailyHalted() {
        when(orderAttemptMapper.countBySideAndCreatedAtBetween(any(), any(), any())).thenReturn(0);
        RiskDailyState daily = new RiskDailyState();
        daily.setBuyHalted(true);
        daily.setHaltReason("일일 손실 -3%");
        when(dailyStateService.getOrCreateToday(any())).thenReturn(daily);

        RiskCheckResult r =
                engine.validateOrder(
                        new RiskManagementEngine.OrderRiskContext(
                                "u1", "005930", "BUY", null, 100_000, 2));
        assertFalse(r.passed());
        assertTrue(r.failReason().contains("신규 매수 중지"));
    }
}
