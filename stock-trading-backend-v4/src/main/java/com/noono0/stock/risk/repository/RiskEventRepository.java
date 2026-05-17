package com.noono0.stock.risk.repository;

import com.noono0.stock.risk.domain.RiskEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {
    List<RiskEvent> findTop50ByOrderByCreatedAtDesc();

    List<RiskEvent> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime after);
}
