package com.noono0.stock.strategy.repository;

import com.noono0.stock.strategy.domain.StrategyMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StrategyMasterRepository extends JpaRepository<StrategyMaster, Long> {
    Optional<StrategyMaster> findByStrategyType(String strategyType);

    List<StrategyMaster> findAllByOrderBySortOrderAscIdAsc();

    boolean existsByStrategyType(String strategyType);
}
