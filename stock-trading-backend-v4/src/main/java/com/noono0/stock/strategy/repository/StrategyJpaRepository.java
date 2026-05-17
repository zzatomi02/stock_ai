package com.noono0.stock.strategy.repository;

import com.noono0.stock.strategy.domain.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StrategyJpaRepository extends JpaRepository<Strategy, Long> {
    Optional<Strategy> findByCode(String code);

    List<Strategy> findAllByOrderBySortOrderAscIdAsc();

    boolean existsByCode(String code);
}
