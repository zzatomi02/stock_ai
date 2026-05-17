package com.noono0.stock.strategy.repository;

import com.noono0.stock.strategy.domain.StrategyTimeWindowRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StrategyTimeWindowRuleRepository extends JpaRepository<StrategyTimeWindowRule, Long> {
    Optional<StrategyTimeWindowRule> findByStrategyTypeAndMarketTimeWindow(
            String strategyType, String marketTimeWindow);

    List<StrategyTimeWindowRule> findByStrategyType(String strategyType);

    List<StrategyTimeWindowRule> findByMarketTimeWindow(String marketTimeWindow);

    List<StrategyTimeWindowRule> findByStrategyTypeAndMarketTimeWindowOrderByIdAsc(
            String strategyType, String marketTimeWindow);
}
