package com.noono0.stock.market.repository;

import com.noono0.stock.market.domain.MarketTop100Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketTop100SnapshotRepository extends JpaRepository<MarketTop100Snapshot, Long> {
    Optional<MarketTop100Snapshot> findByType(String type);
}
