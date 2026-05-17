package com.noono0.stock.strategy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "strategy_time_window",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_strategy_time_window",
                        columnNames = {"strategy_type", "market_time_window"}))
@Getter
@Setter
public class StrategyTimeWindowRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "strategy_type", nullable = false, length = 50)
    private String strategyType;

    @Column(name = "market_time_window", nullable = false, length = 50)
    private String marketTimeWindow;

    @Column(name = "window_name", nullable = false, length = 100)
    private String windowName;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "weight_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightMultiplier = BigDecimal.ONE;

    @Column(name = "min_score_override", precision = 5, scale = 2)
    private BigDecimal minScoreOverride;

    @Column(name = "max_trade_count")
    private Integer maxTradeCount;

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
