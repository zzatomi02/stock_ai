package com.noono0.stock.tradingflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 장마감 매매 복기 (영구 저장) */
@Entity
@Table(
        name = "ai_trade_review",
        indexes = @Index(name = "idx_atr_date_stock", columnList = "trade_date, stock_code"))
@Getter
@Setter
public class AiTradeReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "stock_name", length = 120)
    private String stockName;

    @Column(name = "strategy_code", length = 40)
    private String strategyCode;

    @Column(name = "strategy_signal_id")
    private Long strategySignalId;

    @Column(name = "review_summary", columnDefinition = "TEXT")
    private String reviewSummary;

    @Column(name = "lessons_learned", columnDefinition = "TEXT")
    private String lessonsLearned;

    @Column(name = "ai_decision", length = 50)
    private String aiDecision;

    @Column(name = "final_score")
    private Integer finalScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
