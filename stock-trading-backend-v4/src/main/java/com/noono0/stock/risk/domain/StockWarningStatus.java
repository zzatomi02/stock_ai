package com.noono0.stock.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 종목별 현재 경보·위험 상태 */
@Entity
@Table(
        name = "stock_warning_status",
        uniqueConstraints = @UniqueConstraint(name = "uk_stock_warning", columnNames = "stock_code"))
@Getter
@Setter
public class StockWarningStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "stock_name", length = 120)
    private String stockName;

    @Column(name = "warning_type", nullable = false, length = 40)
    private String warningType;

    @Column(name = "risk_score_adjustment", nullable = false)
    private int riskScoreAdjustment;

    @Column(name = "buy_blocked", nullable = false)
    private boolean buyBlocked;

    @Column(name = "risk_reason", length = 1000)
    private String riskReason;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
