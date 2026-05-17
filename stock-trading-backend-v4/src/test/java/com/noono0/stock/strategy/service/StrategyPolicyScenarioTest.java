package com.noono0.stock.strategy.service;

import com.noono0.stock.strategy.domain.MarketConditionStrategy;
import com.noono0.stock.strategy.domain.StrategyMaster;
import com.noono0.stock.strategy.domain.StrategyRuntimeSetting;
import com.noono0.stock.strategy.domain.StrategyTimeWindowRule;
import com.noono0.stock.strategy.domain.enums.MarketCondition;
import com.noono0.stock.strategy.domain.enums.MarketTimeWindow;
import com.noono0.stock.strategy.domain.enums.StrategyType;
import com.noono0.stock.strategy.dto.StrategySelection;
import com.noono0.stock.strategy.repository.MarketConditionStrategyRepository;
import com.noono0.stock.strategy.repository.StrategyMasterRepository;
import com.noono0.stock.strategy.repository.StrategyRuntimeSettingRepository;
import com.noono0.stock.strategy.repository.StrategyTimeWindowRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 정책·시나리오 검증 (더미 DB 규칙 기반).
 *
 * <p>시드({@link com.noono0.stock.strategy.config.StrategyMasterSeedRunner})와 동일한 규칙을 Mockito로 주입한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StrategyPolicyScenarioTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 17);

    @Mock private StrategyMasterRepository masterRepository;
    @Mock private StrategyRuntimeSettingRepository runtimeRepository;
    @Mock private MarketConditionStrategyRepository marketConditionRepository;
    @Mock private StrategyTimeWindowRuleRepository timeWindowRepository;

    @InjectMocks private StrategySelector strategySelector;

    @BeforeEach
    void setUpMasters() {
        for (StrategyType type : StrategyType.values()) {
            StrategyMaster m = new StrategyMaster();
            m.setStrategyType(type.name());
            m.setStrategyName(type.label());
            m.setEnabled(true);
            lenient()
                    .when(masterRepository.findByStrategyType(type.name()))
                    .thenReturn(Optional.of(m));
        }
        lenient().when(runtimeRepository.findByTradeDateAndStrategyType(eq(TODAY), any())).thenReturn(Optional.empty());
    }

    private void stubMarket(MarketCondition mc, StrategyType st, boolean enabled, String weight) {
        MarketConditionStrategy r = new MarketConditionStrategy();
        r.setId(1L);
        r.setMarketCondition(mc.name());
        r.setStrategyType(st.name());
        r.setEnabled(enabled);
        r.setWeightMultiplier(new BigDecimal(weight));
        when(marketConditionRepository.findByMarketConditionAndStrategyType(mc.name(), st.name()))
                .thenReturn(Optional.of(r));
    }

    private void stubTime(StrategyType st, MarketTimeWindow w, boolean enabled, String weight) {
        StrategyTimeWindowRule r = new StrategyTimeWindowRule();
        r.setId(1L);
        r.setStrategyType(st.name());
        r.setMarketTimeWindow(w.name());
        r.setEnabled(enabled);
        r.setWeightMultiplier(new BigDecimal(weight));
        when(timeWindowRepository.findByStrategyTypeAndMarketTimeWindow(st.name(), w.name()))
                .thenReturn(Optional.of(r));
    }

    private void seedStandardRules() {
        for (StrategyType st : StrategyType.values()) {
            for (MarketCondition mc : MarketCondition.values()) {
                boolean enabled = mc != MarketCondition.BEAR || st == StrategyType.RISK_EXIT;
                String w =
                        (mc == MarketCondition.BEAR && st != StrategyType.RISK_EXIT) ? "0.00" : "1.00";
                if (mc == MarketCondition.BEAR) {
                    if (st == StrategyType.RISK_EXIT) {
                        w = "1.50";
                    }
                }
                stubMarket(mc, st, enabled, w);
            }
        }
        for (StrategyType st : StrategyType.values()) {
            for (MarketTimeWindow w : MarketTimeWindow.values()) {
                boolean enabled = true;
                BigDecimal weight = BigDecimal.ONE;
                if (w == MarketTimeWindow.OPENING_NO_TRADE
                        && st != StrategyType.RISK_EXIT
                        && st != StrategyType.NEWS_THEME
                        && st.buyStrategy()) {
                    enabled = false;
                    weight = BigDecimal.ZERO;
                } else if (w == MarketTimeWindow.OPENING_BET && st == StrategyType.OPENING_BET) {
                    weight = new BigDecimal("1.20");
                } else if (w == MarketTimeWindow.CLOSING_BET && st == StrategyType.CLOSING_BET) {
                    weight = new BigDecimal("1.40");
                }
                stubTime(st, w, enabled, weight.toPlainString());
            }
        }
        stubMarket(MarketCondition.BULL, StrategyType.OPENING_BET, true, "1.20");
        stubMarket(MarketCondition.BULL, StrategyType.SUPPLY_SCALPING, true, "1.20");
        stubMarket(MarketCondition.BEAR, StrategyType.BREAKOUT, false, "0.00");
        stubMarket(MarketCondition.BEAR, StrategyType.OPENING_BET, false, "0.00");
        stubMarket(MarketCondition.BEAR, StrategyType.CLOSING_BET, false, "0.00");
        stubMarket(MarketCondition.BEAR, StrategyType.SUPPLY_SCALPING, false, "0.00");
        stubMarket(MarketCondition.BEAR, StrategyType.RISK_EXIT, true, "1.50");
    }

    private StrategySelection select(StrategyType type, MarketCondition mc, MarketTimeWindow window) {
        StrategyMaster master = masterRepository.findByStrategyType(type.name()).orElseThrow();
        return strategySelector.select(master, TODAY, mc, window);
    }

    @Test
    @DisplayName("시나리오1: 09:01 OPENING_NO_TRADE — 시가베팅·매수전략 비활성, 위험청산 활성")
    void scenario1_openingNoTrade_blocksBuyStrategies() {
        seedStandardRules();
        assertEquals(MarketTimeWindow.OPENING_NO_TRADE, MarketTimeWindow.current(LocalTime.of(9, 1)));

        StrategySelection opening = select(StrategyType.OPENING_BET, MarketCondition.BULL, MarketTimeWindow.OPENING_NO_TRADE);
        assertFalse(opening.executable());
        assertTrue(opening.failedChecks().contains("strategy_time_window"));

        StrategySelection supply =
                select(StrategyType.SUPPLY_SCALPING, MarketCondition.BULL, MarketTimeWindow.OPENING_NO_TRADE);
        assertFalse(supply.executable());

        StrategySelection risk = select(StrategyType.RISK_EXIT, MarketCondition.BULL, MarketTimeWindow.OPENING_NO_TRADE);
        assertTrue(risk.executable(), "위험청산은 장중 전 시간대 활성");
    }

    @Test
    @DisplayName("시나리오2: 09:10 OPENING_BET + BULL — 시가·수급 활성, 가중치 적용")
    void scenario2_openingBet_bull_enabledWithWeights() {
        seedStandardRules();
        assertEquals(MarketTimeWindow.OPENING_BET, MarketTimeWindow.current(LocalTime.of(9, 10)));

        StrategySelection opening = select(StrategyType.OPENING_BET, MarketCondition.BULL, MarketTimeWindow.OPENING_BET);
        assertTrue(opening.executable());
        assertEquals(1.20, opening.marketWeightMultiplier(), 0.01);
        assertEquals(1.20, opening.timeWeightMultiplier(), 0.01);
        assertEquals(1.44, opening.combinedWeightMultiplier(), 0.01);

        StrategySelection supply = select(StrategyType.SUPPLY_SCALPING, MarketCondition.BULL, MarketTimeWindow.OPENING_BET);
        assertTrue(supply.executable());
    }

    @Test
    @DisplayName("시나리오3: BEAR — 매수 전략 OFF, 위험청산 ON")
    void scenario3_bear_disablesBuyStrategies() {
        seedStandardRules();

        assertFalse(select(StrategyType.BREAKOUT, MarketCondition.BEAR, MarketTimeWindow.MORNING_MARKET).executable());
        assertFalse(select(StrategyType.OPENING_BET, MarketCondition.BEAR, MarketTimeWindow.MORNING_MARKET).executable());
        assertFalse(select(StrategyType.CLOSING_BET, MarketCondition.BEAR, MarketTimeWindow.MORNING_MARKET).executable());
        assertFalse(select(StrategyType.SUPPLY_SCALPING, MarketCondition.BEAR, MarketTimeWindow.MORNING_MARKET).executable());

        StrategySelection risk = select(StrategyType.RISK_EXIT, MarketCondition.BEAR, MarketTimeWindow.MORNING_MARKET);
        assertTrue(risk.executable());
        assertEquals(1.50, risk.marketWeightMultiplier(), 0.01);
    }

    @Test
    @DisplayName("시나리오4: 15:15 CLOSING_BET — 종가베팅 활성 가중치 1.40, 시가베팅 비활성")
    void scenario4_closingBet_window() {
        seedStandardRules();
        assertEquals(MarketTimeWindow.CLOSING_BET, MarketTimeWindow.current(LocalTime.of(15, 15)));

        StrategySelection closing = select(StrategyType.CLOSING_BET, MarketCondition.BULL, MarketTimeWindow.CLOSING_BET);
        assertTrue(closing.executable());
        assertEquals(1.40, closing.timeWeightMultiplier(), 0.01);

        stubTime(StrategyType.OPENING_BET, MarketTimeWindow.CLOSING_BET, false, "0.00");
        StrategySelection opening = select(StrategyType.OPENING_BET, MarketCondition.BULL, MarketTimeWindow.CLOSING_BET);
        assertFalse(opening.executable());
    }

    @Test
    @DisplayName("시나리오5: 오늘만 OPENING_BET OFF — 시장·시간 좋아도 실행 불가")
    void scenario5_runtimeTodayOff_openingBet() {
        seedStandardRules();
        StrategyRuntimeSetting runtime = new StrategyRuntimeSetting();
        runtime.setTradeDate(TODAY);
        runtime.setStrategyType(StrategyType.OPENING_BET.name());
        runtime.setEnabled(false);
        runtime.setReason("오늘 장초반 변동성");
        when(runtimeRepository.findByTradeDateAndStrategyType(TODAY, StrategyType.OPENING_BET.name()))
                .thenReturn(Optional.of(runtime));

        StrategySelection opening =
                select(StrategyType.OPENING_BET, MarketCondition.BULL, MarketTimeWindow.OPENING_BET);
        assertFalse(opening.executable());
        assertTrue(opening.failedChecks().contains("strategy_runtime_setting"));
    }

    @Test
    @DisplayName("정책6: 설정 없으면 기본값(ON, weight=1)으로 동작")
    void defaultRules_whenNoDbRow() {
        StrategyMaster master = new StrategyMaster();
        master.setStrategyType(StrategyType.PULLBACK.name());
        master.setEnabled(true);
        when(masterRepository.findByStrategyType(StrategyType.PULLBACK.name())).thenReturn(Optional.of(master));
        when(marketConditionRepository.findByMarketConditionAndStrategyType(
                        MarketCondition.BULL.name(), StrategyType.PULLBACK.name()))
                .thenReturn(Optional.empty());
        when(timeWindowRepository.findByStrategyTypeAndMarketTimeWindow(
                        StrategyType.PULLBACK.name(), MarketTimeWindow.MORNING_MARKET.name()))
                .thenReturn(Optional.empty());

        StrategySelection sel =
                strategySelector.select(master, TODAY, MarketCondition.BULL, MarketTimeWindow.MORNING_MARKET);
        assertTrue(sel.executable());
        assertEquals(1.0, sel.combinedWeightMultiplier(), 0.01);
    }
}
