package com.noono0.stock.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 경보 등록·해제 이력 */
@Entity
@Table(name = "market_warning_history", indexes = @Index(name = "idx_mwh_stock_time", columnList = "stock_code, recorded_at"))
@Getter
@Setter
public class MarketWarningHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "warning_type", nullable = false, length = 40)
    private String warningType;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "risk_reason", length = 1000)
    private String riskReason;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    void pre() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }
}
