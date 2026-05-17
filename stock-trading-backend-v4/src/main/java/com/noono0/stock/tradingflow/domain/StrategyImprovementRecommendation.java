package com.noono0.stock.tradingflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "strategy_improvement_recommendation")
@Getter
@Setter
public class StrategyImprovementRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "strategy_type", length = 40)
    private String strategyType;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "evidence_summary", columnDefinition = "TEXT")
    private String evidenceSummary;

    /** PENDING | APPROVED | REJECTED */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
