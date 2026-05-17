package com.noono0.stock.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_reason")
@Getter
@Setter
public class OrderReason {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "signal_id")
    private Long signalId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "reason_type", nullable = false, length = 30)
    private String reasonType;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "news_reason", columnDefinition = "TEXT")
    private String newsReason;

    @Column(name = "keyword_reason", columnDefinition = "TEXT")
    private String keywordReason;

    @Column(name = "technical_reason", columnDefinition = "TEXT")
    private String technicalReason;

    @Column(name = "market_reason", columnDefinition = "TEXT")
    private String marketReason;

    @Column(name = "risk_reason", columnDefinition = "TEXT")
    private String riskReason;

    @Column(name = "strategy_reason", columnDefinition = "TEXT")
    private String strategyReason;

    @Column(name = "reason_json", columnDefinition = "TEXT")
    private String reasonJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
