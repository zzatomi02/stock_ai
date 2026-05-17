package com.noono0.stock.strategy.repository;

import com.noono0.stock.strategy.domain.StrategyConfigChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StrategyConfigChangeRequestRepository extends JpaRepository<StrategyConfigChangeRequest, Long> {
    List<StrategyConfigChangeRequest> findByStatusOrderByCreatedAtDesc(String status);
}
