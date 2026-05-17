package com.noono0.stock.strategy.repository;

import com.noono0.stock.strategy.domain.StrategyTimeSlotRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StrategyTimeSlotRuleRepository extends JpaRepository<StrategyTimeSlotRule, Long> {
    List<StrategyTimeSlotRule> findByStrategyCode(String strategyCode);

    Optional<StrategyTimeSlotRule> findByStrategyCodeAndTimeSlotCode(
            String strategyCode, String timeSlotCode);
}
