package com.noono0.stock.risk.repository;

import com.noono0.stock.risk.domain.StockWarningStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockWarningStatusRepository extends JpaRepository<StockWarningStatus, Long> {
    Optional<StockWarningStatus> findByStockCodeAndActiveTrue(String stockCode);

    Optional<StockWarningStatus> findByStockCode(String stockCode);

    List<StockWarningStatus> findByActiveTrueOrderByStockCodeAsc();
}
