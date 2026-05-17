package com.noono0.stock.risk.repository;

import com.noono0.stock.risk.domain.RiskStrategyDailyState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RiskStrategyDailyStateRepository extends JpaRepository<RiskStrategyDailyState, Long> {
    Optional<RiskStrategyDailyState> findByTradeDateAndStrategyType(LocalDate tradeDate, String strategyType);
}
