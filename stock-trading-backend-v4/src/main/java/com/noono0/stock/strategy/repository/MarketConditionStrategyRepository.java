package com.noono0.stock.strategy.repository;

import com.noono0.stock.strategy.domain.MarketConditionStrategy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketConditionStrategyRepository extends JpaRepository<MarketConditionStrategy, Long> {
    Optional<MarketConditionStrategy> findByMarketConditionAndStrategyType(
            String marketCondition, String strategyType);

    List<MarketConditionStrategy> findByMarketCondition(String marketCondition);

    List<MarketConditionStrategy> findByStrategyType(String strategyType);

    List<MarketConditionStrategy> findByMarketConditionAndStrategyTypeOrderByIdAsc(
            String marketCondition, String strategyType);
}
