package com.noono0.stock.market.repository;

import com.noono0.stock.market.domain.MarketMoodSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketMoodSnapshotRepository extends JpaRepository<MarketMoodSnapshot, Long> {
    Optional<MarketMoodSnapshot> findTopByOrderByCreatedAtDesc();
}
