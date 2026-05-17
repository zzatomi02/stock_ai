package com.noono0.stock.strategy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "strategy_runtime_setting",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_runtime_date_type",
                        columnNames = {"trade_date", "strategy_type"}))
@Getter
@Setter
public class StrategyRuntimeSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "strategy_type", nullable = false, length = 50)
    private String strategyType;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled = true;

    @Column(length = 500)
    private String reason;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

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
