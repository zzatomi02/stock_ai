package com.noono0.stock.strategy.config;

import com.noono0.stock.strategy.domain.MarketConditionStrategy;
import com.noono0.stock.strategy.domain.StrategyMaster;
import com.noono0.stock.strategy.domain.StrategyTimeWindowRule;
import com.noono0.stock.strategy.domain.enums.MarketCondition;
import com.noono0.stock.strategy.domain.enums.MarketTimeWindow;
import com.noono0.stock.strategy.domain.enums.StrategyType;
import com.noono0.stock.strategy.repository.MarketConditionStrategyRepository;
import com.noono0.stock.strategy.repository.StrategyMasterRepository;
import com.noono0.stock.strategy.repository.StrategyTimeWindowRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class StrategyMasterSeedRunner implements ApplicationRunner {

    private static final Set<StrategyType> BUY_TYPES =
            Set.of(
                    StrategyType.SUPPLY_SCALPING,
                    StrategyType.OPENING_BET,
                    StrategyType.CLOSING_BET,
                    StrategyType.BREAKOUT,
                    StrategyType.PULLBACK,
                    StrategyType.SHORT_SWING);

    private final StrategyMasterRepository masterRepository;
    private final MarketConditionStrategyRepository marketConditionRepository;
    private final StrategyTimeWindowRuleRepository timeWindowRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int order = 0;
        for (StrategyType type : StrategyType.values()) {
            if (!masterRepository.existsByStrategyType(type.name())) {
                StrategyMaster m = new StrategyMaster();
                m.setStrategyType(type.name());
                m.setStrategyName(type.label());
                m.setDescription(type.label() + " 전략");
                m.setEnabled(true);
                m.setSortOrder(order++);
                masterRepository.save(m);
                log.info("[STRATEGY-MASTER] 등록 {}", type.name());
            }
        }
        seedMarketRules();
        seedTimeWindowRules();
    }

    private void seedMarketRules() {
        upsertMarket(MarketCondition.STRONG_BULL, StrategyType.BREAKOUT, true, "1.30", null);
        upsertMarket(MarketCondition.STRONG_BULL, StrategyType.OPENING_BET, true, "1.20", null);
        upsertMarket(MarketCondition.STRONG_BULL, StrategyType.SUPPLY_SCALPING, true, "1.20", null);
        upsertMarket(MarketCondition.STRONG_BULL, StrategyType.CLOSING_BET, true, "1.10", null);

        upsertMarket(MarketCondition.SIDEWAYS, StrategyType.PULLBACK, true, "1.30", null);
        upsertMarket(MarketCondition.SIDEWAYS, StrategyType.SHORT_SWING, true, "1.10", null);
        upsertMarket(MarketCondition.SIDEWAYS, StrategyType.BREAKOUT, true, "0.70", null);
        upsertMarket(MarketCondition.SIDEWAYS, StrategyType.OPENING_BET, false, "0.00", null);

        upsertMarket(MarketCondition.BEAR, StrategyType.BREAKOUT, false, "0.00", null);
        upsertMarket(MarketCondition.BEAR, StrategyType.OPENING_BET, false, "0.00", null);
        upsertMarket(MarketCondition.BEAR, StrategyType.CLOSING_BET, false, "0.00", null);
        upsertMarket(MarketCondition.BEAR, StrategyType.SUPPLY_SCALPING, false, "0.00", null);
        upsertMarket(MarketCondition.BEAR, StrategyType.RISK_EXIT, true, "1.50", null);

        upsertMarket(MarketCondition.THEME, StrategyType.NEWS_THEME, true, "1.50", null);
        upsertMarket(MarketCondition.THEME, StrategyType.SUPPLY_SCALPING, true, "1.20", null);
        upsertMarket(MarketCondition.THEME, StrategyType.CLOSING_BET, true, "1.20", null);

        upsertMarket(MarketCondition.BULL, StrategyType.OPENING_BET, true, "1.20", null);
        upsertMarket(MarketCondition.BULL, StrategyType.SUPPLY_SCALPING, true, "1.20", null);
        upsertMarket(MarketCondition.BULL, StrategyType.BREAKOUT, true, "1.10", null);
        upsertMarket(MarketCondition.BULL, StrategyType.CLOSING_BET, true, "1.00", null);

        for (MarketCondition mc : MarketCondition.values()) {
            for (StrategyType st : StrategyType.values()) {
                if (marketConditionRepository
                        .findByMarketConditionAndStrategyType(mc.name(), st.name())
                        .isEmpty()) {
                    boolean enabled = mc != MarketCondition.BEAR || st == StrategyType.RISK_EXIT;
                    double w = enabled ? 1.0 : 0.0;
                    if (mc == MarketCondition.BEAR && BUY_TYPES.contains(st)) {
                        enabled = false;
                        w = 0;
                    }
                    upsertMarket(mc, st, enabled, String.format("%.2f", w), null);
                }
            }
        }
    }

    private void upsertMarket(
            MarketCondition mc, StrategyType st, boolean enabled, String weight, String minOverride) {
        MarketConditionStrategy r =
                marketConditionRepository
                        .findByMarketConditionAndStrategyType(mc.name(), st.name())
                        .orElseGet(
                                () -> {
                                    MarketConditionStrategy n = new MarketConditionStrategy();
                                    n.setMarketCondition(mc.name());
                                    n.setStrategyType(st.name());
                                    return n;
                                });
        r.setEnabled(enabled);
        r.setWeightMultiplier(new BigDecimal(weight));
        if (minOverride != null) {
            r.setMinScoreOverride(new BigDecimal(minOverride));
        }
        marketConditionRepository.save(r);
    }

    private void seedTimeWindowRules() {
        for (StrategyType st : StrategyType.values()) {
            for (MarketTimeWindow w : MarketTimeWindow.values()) {
                boolean enabled = true;
                BigDecimal weight = BigDecimal.ONE;

                if (w == MarketTimeWindow.OPENING_NO_TRADE && BUY_TYPES.contains(st)) {
                    enabled = false;
                    weight = BigDecimal.ZERO;
                } else if (w == MarketTimeWindow.OPENING_BET && st == StrategyType.OPENING_BET) {
                    weight = new BigDecimal("1.20");
                } else if (w == MarketTimeWindow.OPENING_BET
                        && (st == StrategyType.SUPPLY_SCALPING || st == StrategyType.BREAKOUT)) {
                    weight = new BigDecimal("1.10");
                } else if (w == MarketTimeWindow.CLOSING_BET && st == StrategyType.CLOSING_BET) {
                    weight = new BigDecimal("1.40");
                } else if (w == MarketTimeWindow.CLOSING_PREPARE && st == StrategyType.CLOSING_BET) {
                    weight = new BigDecimal("1.15");
                } else if (w == MarketTimeWindow.LUNCH_MARKET
                        && (st == StrategyType.SUPPLY_SCALPING || st == StrategyType.BREAKOUT)) {
                    weight = new BigDecimal("0.50");
                }

                if (st == StrategyType.RISK_EXIT) {
                    enabled = true;
                    weight = BigDecimal.ONE;
                }
                if (st == StrategyType.NEWS_THEME) {
                    enabled = true;
                    weight = BigDecimal.ONE;
                }

                upsertTimeWindow(st, w, enabled, weight);
            }
        }
    }

    private void upsertTimeWindow(
            StrategyType st, MarketTimeWindow w, boolean enabled, BigDecimal weight) {
        StrategyTimeWindowRule rule =
                timeWindowRepository
                        .findByStrategyTypeAndMarketTimeWindow(st.name(), w.name())
                        .orElseGet(
                                () -> {
                                    StrategyTimeWindowRule n = new StrategyTimeWindowRule();
                                    n.setStrategyType(st.name());
                                    n.setMarketTimeWindow(w.name());
                                    return n;
                                });
        rule.setWindowName(w.label());
        rule.setStartTime(w.start());
        rule.setEndTime(w.end());
        rule.setEnabled(enabled);
        rule.setWeightMultiplier(weight);
        timeWindowRepository.save(rule);
    }
}
