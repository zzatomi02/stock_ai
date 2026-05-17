package com.noono0.stock.strategy.service;

import com.noono0.stock.strategy.domain.*;
import com.noono0.stock.strategy.domain.enums.MarketCondition;
import com.noono0.stock.strategy.domain.enums.MarketTimeWindow;
import com.noono0.stock.strategy.dto.StrategySelection;
import com.noono0.stock.strategy.domain.enums.StrategyType;
import com.noono0.stock.strategy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * 전략 실행 가능 여부 최종 판단.
 *
 * <p>아래 4가지를 <strong>모두</strong> 통과해야 {@link StrategySelection#executable()} 이 true 이다.
 *
 * <ol>
 *   <li>{@code strategy_master} 기본 ON/OFF
 *   <li>{@code strategy_runtime_setting} 오늘 임시 ON/OFF (행이 있으면 그 값, 없으면 통과)
 *   <li>{@code market_condition_strategy} 시장상태별 ON/OFF
 *   <li>{@code strategy_time_window} 시간대별 ON/OFF
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategySelector {

    private final StrategyMasterRepository masterRepository;
    private final StrategyRuntimeSettingRepository runtimeRepository;
    private final MarketConditionStrategyRepository marketConditionRepository;
    private final StrategyTimeWindowRuleRepository timeWindowRepository;

    public StrategySelection select(
            String strategyType, LocalDate tradeDate, MarketCondition condition, LocalTime time) {
        StrategyMaster master =
                masterRepository
                        .findByStrategyType(strategyType)
                        .orElseThrow(() -> new IllegalArgumentException("전략을 찾을 수 없습니다: " + strategyType));
        return select(master, tradeDate, condition, MarketTimeWindow.current(time));
    }

    public StrategySelection select(
            StrategyMaster master,
            LocalDate tradeDate,
            MarketCondition condition,
            MarketTimeWindow window) {
        boolean masterEnabled = Boolean.TRUE.equals(master.getEnabled());

        TodayRuntimeGate todayGate = resolveTodayRuntime(master.getStrategyType(), tradeDate);

        var marketLookup =
                marketConditionRepository.findByMarketConditionAndStrategyType(
                        condition.name(), master.getStrategyType());
        boolean marketDefault = marketLookup.isEmpty();
        MarketConditionStrategy marketRule =
                marketLookup.orElseGet(() -> defaultMarketRule(master.getStrategyType(), condition));
        if (marketDefault) {
            log.info(
                    "[STRATEGY-DEFAULT] market_condition_strategy 없음 → 기본값 사용 (enabled=true, weight=1.0) | 시장={} 전략={}",
                    condition.name(),
                    master.getStrategyType());
        }
        boolean marketRuleEnabled = Boolean.TRUE.equals(marketRule.getEnabled());
        double marketWeight = toDouble(marketRule.getWeightMultiplier());

        var timeLookup =
                timeWindowRepository.findByStrategyTypeAndMarketTimeWindow(
                        master.getStrategyType(), window.name());
        boolean timeDefault = timeLookup.isEmpty();
        StrategyTimeWindowRule timeRule =
                timeLookup.orElseGet(() -> defaultTimeRule(master.getStrategyType(), window));
        if (timeDefault) {
            log.info(
                    "[STRATEGY-DEFAULT] strategy_time_window 없음 → 기본값 사용 (enabled=true, weight=1.0) | 시간={} 전략={}",
                    window.name(),
                    master.getStrategyType());
        }
        boolean timeWindowEnabled = Boolean.TRUE.equals(timeRule.getEnabled());
        double timeWeight = toDouble(timeRule.getWeightMultiplier());

        if (StrategyType.RISK_EXIT.name().equals(master.getStrategyType())) {
            timeWindowEnabled = true;
            if (timeWeight < 1.0) {
                timeWeight = 1.0;
            }
        }

        return StrategySelection.of(
                master.getStrategyType(),
                masterEnabled,
                todayGate.hasOverride(),
                todayGate.runtimeEnabled(),
                marketRuleEnabled,
                timeWindowEnabled,
                marketWeight,
                timeWeight);
    }

    private TodayRuntimeGate resolveTodayRuntime(String strategyType, LocalDate tradeDate) {
        Optional<StrategyRuntimeSetting> runtime =
                runtimeRepository.findByTradeDateAndStrategyType(tradeDate, strategyType);
        if (runtime.isEmpty()) {
            return new TodayRuntimeGate(false, true);
        }
        return new TodayRuntimeGate(true, Boolean.TRUE.equals(runtime.get().getEnabled()));
    }

    private record TodayRuntimeGate(boolean hasOverride, boolean runtimeEnabled) {}

    private static MarketConditionStrategy defaultMarketRule(String strategyType, MarketCondition condition) {
        MarketConditionStrategy r = new MarketConditionStrategy();
        r.setStrategyType(strategyType);
        r.setMarketCondition(condition.name());
        r.setEnabled(true);
        r.setWeightMultiplier(BigDecimal.ONE);
        return r;
    }

    private static StrategyTimeWindowRule defaultTimeRule(String strategyType, MarketTimeWindow window) {
        StrategyTimeWindowRule r = new StrategyTimeWindowRule();
        r.setStrategyType(strategyType);
        r.setMarketTimeWindow(window.name());
        r.setWindowName(window.label());
        r.setStartTime(window.start());
        r.setEndTime(window.end());
        r.setEnabled(true);
        r.setWeightMultiplier(BigDecimal.ONE);
        return r;
    }

    private static double toDouble(BigDecimal v) {
        return v == null ? 1.0 : v.doubleValue();
    }
}
