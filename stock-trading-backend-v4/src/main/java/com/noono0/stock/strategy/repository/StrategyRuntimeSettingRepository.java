package com.noono0.stock.strategy.repository;

import com.noono0.stock.strategy.domain.StrategyRuntimeSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface StrategyRuntimeSettingRepository extends JpaRepository<StrategyRuntimeSetting, Long> {
    Optional<StrategyRuntimeSetting> findByTradeDateAndStrategyType(LocalDate tradeDate, String strategyType);
}
