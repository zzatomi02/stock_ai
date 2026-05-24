package com.noono0.stock.recommendation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 전략 시그널 기반 일별 종목 추천 (종목 단위 집계). */
@Entity
@Table(
        name = "stock_recommendation",
        uniqueConstraints = @UniqueConstraint(name = "uk_stock_rec_date_code", columnNames = {"trade_date", "stock_code"}),
        indexes = @Index(name = "idx_stock_rec_status", columnList = "trade_date, status"))
@Getter
@Setter
public class StockRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "stock_name", length = 120)
    private String stockName;

    /**
     * RECOMMENDED(관찰) | PENDING_APPROVAL | APPROVED | REJECTED | ORDERED | ORDER_FAILED
     */
    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "best_adjusted_score", precision = 5, scale = 2)
    private BigDecimal bestAdjustedScore;

    @Column(name = "signal_grade", length = 2)
    private String signalGrade;

    @Column(name = "strategy_codes", length = 500)
    private String strategyCodes;

    @Column(name = "primary_strategy_signal_id")
    private Long primaryStrategySignalId;

    @Column(name = "reason_summary", length = 2000)
    private String reasonSummary;

    @Column(name = "notification_sent", nullable = false)
    private Boolean notificationSent = false;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 64)
    private String approvedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by", length = 64)
    private String rejectedBy;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "broker_order_attempt_id")
    private Long brokerOrderAttemptId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (notificationSent == null) {
            notificationSent = false;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
