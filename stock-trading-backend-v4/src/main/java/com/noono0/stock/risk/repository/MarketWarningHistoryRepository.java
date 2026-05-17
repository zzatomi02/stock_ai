package com.noono0.stock.risk.repository;

import com.noono0.stock.risk.domain.MarketWarningHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketWarningHistoryRepository extends JpaRepository<MarketWarningHistory, Long> {
    List<MarketWarningHistory> findTop100ByStockCodeOrderByRecordedAtDesc(String stockCode);
}
