package com.noono0.stock.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 전략별 일일 리스크(매매 횟수·연속 손절) */
@Entity
@Table(
        name = "risk_strategy_daily_state",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_risk_strategy_daily",
                        columnNames = {"trade_date", "strategy_type"}))
@Getter
@Setter
public class RiskStrategyDailyState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "strategy_type", nullable = false, length = 40)
    private String strategyType;

    @Column(name = "trade_count", nullable = false)
    private int tradeCount;

    @Column(name = "consecutive_stop_loss", nullable = false)
    private int consecutiveStopLoss;

    @Column(name = "disabled_today", nullable = false)
    private boolean disabledToday;

    @Column(name = "disabled_reason", length = 500)
    private String disabledReason;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
