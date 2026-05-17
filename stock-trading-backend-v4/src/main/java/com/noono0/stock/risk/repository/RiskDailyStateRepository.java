package com.noono0.stock.risk.repository;

import com.noono0.stock.risk.domain.RiskDailyState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RiskDailyStateRepository extends JpaRepository<RiskDailyState, Long> {
    Optional<RiskDailyState> findByTradeDateAndUserId(LocalDate tradeDate, String userId);

    default Optional<RiskDailyState> findToday(String userId) {
        return findByTradeDateAndUserId(LocalDate.now(), userId);
    }
}
