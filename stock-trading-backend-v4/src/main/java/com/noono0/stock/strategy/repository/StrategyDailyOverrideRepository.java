package com.noono0.stock.strategy.repository;

import com.noono0.stock.strategy.domain.StrategyDailyOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface StrategyDailyOverrideRepository extends JpaRepository<StrategyDailyOverride, Long> {
    Optional<StrategyDailyOverride> findByStrategyCodeAndTradeDate(String strategyCode, LocalDate tradeDate);
}
