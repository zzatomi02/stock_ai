package com.noono0.stock.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 일별 리스크 집계(손실·매매 횟수·매수 중지 플래그) */
@Entity
@Table(
        name = "risk_daily_state",
        uniqueConstraints = @UniqueConstraint(name = "uk_risk_daily", columnNames = {"trade_date", "user_id"}))
@Getter
@Setter
public class RiskDailyState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "reference_equity", precision = 18, scale = 2)
    private BigDecimal referenceEquity;

    @Column(name = "realized_pnl", precision = 18, scale = 2)
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @Column(name = "buy_order_count", nullable = false)
    private int buyOrderCount;

    @Column(name = "sell_order_count", nullable = false)
    private int sellOrderCount;

    @Column(name = "buy_halted", nullable = false)
    private boolean buyHalted;

    @Column(name = "global_halt", nullable = false)
    private boolean globalHalt;

    @Column(name = "halt_reason", length = 500)
    private String haltReason;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
