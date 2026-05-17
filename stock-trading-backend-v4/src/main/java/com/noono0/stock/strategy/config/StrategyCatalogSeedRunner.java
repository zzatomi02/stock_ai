package com.noono0.stock.strategy.config;

import com.noono0.stock.strategy.domain.*;
import com.noono0.stock.strategy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/** @deprecated strategy_master / market_condition_strategy / strategy_time_window 사용 */
@Slf4j
// @Component — StrategyMasterSeedRunner 로 대체
@Order(20)
@RequiredArgsConstructor
public class StrategyCatalogSeedRunner implements ApplicationRunner {
    private static final Set<StrategyCode> BUY_STRATEGIES =
            Set.of(
                    StrategyCode.SUPPLY_SCALP,
                    StrategyCode.CLOSE_BET,
                    StrategyCode.OPEN_BET,
                    StrategyCode.BREAKOUT,
                    StrategyCode.PULLBACK,
                    StrategyCode.SHORT_SWING);

    private final StrategyJpaRepository strategyRepository;
    private final StrategyMarketRegimeRuleRepository marketRuleRepository;
    private final StrategyTimeSlotRuleRepository timeSlotRuleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int order = 0;
        for (StrategyCode code : StrategyCode.values()) {
            if (!strategyRepository.existsByCode(code.name())) {
                Strategy s = new Strategy();
                s.setCode(code.name());
                s.setName(code.label());
                s.setCategory(code.category());
                s.setSortOrder(order++);
                s.setEnabled(defaultBaseEnabled(code));
                s.setTargetMarket("KOSPI,KOSDAQ");
                s.setRiskLimitRate(3.0);
                s.setTakeProfitRate(5.0);
                s.setMaxBuyAmount(1_000_000L);
                s.setKeywordScoreEnabled(true);
                s.setAiScoreEnabled(true);
                s.setAutoTradeEnabled(false);
                s.setReentryCooldownMinutes(30);
                s.setMinScoreThreshold(defaultMinScore(code));
                s.setCreatedAt(LocalDateTime.now());
                s.setUpdatedAt(LocalDateTime.now());
                strategyRepository.save(s);
                seedMarketRules(code);
                seedTimeRules(code);
                log.info("[STRATEGY-SEED] 등록: {}", code.name());
            } else {
                strategyRepository
                        .findByCode(code.name())
                        .ifPresent(
                                s -> {
                                    if (s.getMinScoreThreshold() == null) {
                                        s.setMinScoreThreshold(defaultMinScore(code));
                                        strategyRepository.save(s);
                                    }
                                });
            }
        }
    }

    private static int defaultMinScore(StrategyCode code) {
        return switch (code) {
            case OPEN_BET -> 70;
            case CLOSE_BET -> 65;
            case BREAKOUT -> 75;
            case SUPPLY_SCALP -> 72;
            case PULLBACK -> 68;
            case SHORT_SWING -> 65;
            case RISK_LIQUIDATION -> 45;
            case NEWS_THEME, ORDER_REASON -> 50;
            default -> 62;
        };
    }

    private static boolean defaultBaseEnabled(StrategyCode code) {
        return code != StrategyCode.PAPER_TRADE;
    }

    private void seedMarketRules(StrategyCode code) {
        for (MarketRegime regime : MarketRegime.values()) {
            if (marketRuleRepository
                    .findByStrategyCodeAndMarketRegime(code.name(), regime)
                    .isPresent()) {
                continue;
            }
            boolean enabled = true;
            double weight = 1.0;

            if (regime == MarketRegime.STRONG_BULL) {
                if (code == StrategyCode.BREAKOUT
                        || code == StrategyCode.OPEN_BET
                        || code == StrategyCode.SUPPLY_SCALP) {
                    weight = 1.3;
                }
            } else if (regime == MarketRegime.BEAR || regime == MarketRegime.STRONG_BEAR) {
                if (BUY_STRATEGIES.contains(code)) {
                    enabled = false;
                    weight = 0;
                }
                if (code == StrategyCode.RISK_LIQUIDATION) {
                    enabled = true;
                    weight = 1.5;
                }
            } else if (regime == MarketRegime.NEUTRAL) {
                if (code == StrategyCode.SUPPLY_SCALP || code == StrategyCode.BREAKOUT) {
                    weight = 0.8;
                }
            }

            StrategyMarketRegimeRule r = new StrategyMarketRegimeRule();
            r.setStrategyCode(code.name());
            r.setMarketRegime(regime);
            r.setEnabled(enabled);
            r.setWeight(weight);
            marketRuleRepository.save(r);
        }
    }

    private void seedTimeRules(StrategyCode code) {
        for (TradingTimeSlot slot : TradingTimeSlot.values()) {
            if (timeSlotRuleRepository
                    .findByStrategyCodeAndTimeSlotCode(code.name(), slot.code())
                    .isPresent()) {
                continue;
            }
            boolean enabled = true;
            double weight = 1.0;
            boolean blockBuy = false;

            switch (slot) {
                case PRE_OPEN_OBSERVE -> {
                    if (BUY_STRATEGIES.contains(code)) {
                        enabled = false;
                        blockBuy = true;
                        weight = 0;
                    }
                }
                case OPEN_BET_WINDOW -> {
                    if (code == StrategyCode.OPEN_BET) {
                        enabled = true;
                        weight = 1.4;
                    } else if (BUY_STRATEGIES.contains(code) && code != StrategyCode.OPEN_BET) {
                        weight = 0.6;
                    }
                }
                case LUNCH -> {
                    if (code == StrategyCode.SUPPLY_SCALP
                            || code == StrategyCode.BREAKOUT
                            || code == StrategyCode.OPEN_BET) {
                        weight = 0.5;
                    }
                }
                case CLOSE_BET_WINDOW -> {
                    if (code == StrategyCode.CLOSE_BET) {
                        enabled = true;
                        weight = 1.4;
                    }
                }
                case CLOSING -> {
                    if (BUY_STRATEGIES.contains(code) && code != StrategyCode.CLOSE_BET) {
                        weight = 0.3;
                    }
                }
                default -> {}
            }

            if (code == StrategyCode.RISK_LIQUIDATION) {
                enabled = true;
                blockBuy = false;
                weight = Math.max(weight, 1.0);
            }
            if (code == StrategyCode.NEWS_THEME || code == StrategyCode.ORDER_REASON) {
                enabled = true;
                blockBuy = false;
                weight = 1.0;
            }

            StrategyTimeSlotRule r = new StrategyTimeSlotRule();
            r.setStrategyCode(code.name());
            r.setTimeSlotCode(slot.code());
            r.setEnabled(enabled);
            r.setWeight(weight);
            r.setBlockBuy(blockBuy);
            timeSlotRuleRepository.save(r);
        }
    }
}
