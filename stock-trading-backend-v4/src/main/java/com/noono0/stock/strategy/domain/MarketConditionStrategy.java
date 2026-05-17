package com.noono0.stock.strategy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "market_condition_strategy",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_market_strategy",
                        columnNames = {"market_condition", "strategy_type"}))
@Getter
@Setter
public class MarketConditionStrategy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "market_condition", nullable = false, length = 50)
    private String marketCondition;

    @Column(name = "strategy_type", nullable = false, length = 50)
    private String strategyType;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "weight_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightMultiplier = BigDecimal.ONE;

    @Column(name = "min_score_override", precision = 5, scale = 2)
    private BigDecimal minScoreOverride;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
