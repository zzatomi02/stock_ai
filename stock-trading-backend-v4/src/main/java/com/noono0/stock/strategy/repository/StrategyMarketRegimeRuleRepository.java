package com.noono0.stock.strategy.repository;

import com.noono0.stock.strategy.domain.MarketRegime;
import com.noono0.stock.strategy.domain.StrategyMarketRegimeRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StrategyMarketRegimeRuleRepository extends JpaRepository<StrategyMarketRegimeRule, Long> {
    List<StrategyMarketRegimeRule> findByStrategyCode(String strategyCode);

    Optional<StrategyMarketRegimeRule> findByStrategyCodeAndMarketRegime(
            String strategyCode, MarketRegime marketRegime);
}
