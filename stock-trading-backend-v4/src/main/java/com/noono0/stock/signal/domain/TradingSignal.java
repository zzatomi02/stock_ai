package com.noono0.stock.signal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "trading_signal",
        indexes = {
            @Index(name = "idx_signal_user_created", columnList = "user_id, created_at"),
            @Index(name = "idx_signal_stock_created", columnList = "stock_code, created_at")
        })
@Getter
@Setter
public class TradingSignal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "stock_name", length = 120)
    private String stockName;

    /** BUY | SELL */
    @Column(nullable = false, length = 8)
    private String side;

    /** CANDIDATE | REJECTED | EXPIRED | APPROVED */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "keyword_score")
    private Integer keywordScore;

    @Column(name = "ai_score")
    private Integer aiScore;

    @Column(name = "final_score")
    private Integer finalScore;

    /** S | A | B | C | D */
    @Column(name = "signal_grade", length = 2)
    private String signalGrade;

    @Column(name = "market_mood_score")
    private Integer marketMoodScore;

    @Column(name = "news_article_id")
    private Long newsArticleId;

    @Column(name = "llm_sentiment", length = 16)
    private String llmSentiment;

    @Column(name = "llm_event_type", length = 32)
    private String llmEventType;

    @Column(name = "llm_action_hint", length = 20)
    private String llmActionHint;

    @Column(name = "llm_confidence")
    private Double llmConfidence;

    @Column(name = "llm_summary", columnDefinition = "TEXT")
    private String llmSummary;

    @Column(name = "reason_snapshot", columnDefinition = "TEXT")
    private String reasonSnapshot;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Column(name = "execution_phase", length = 24)
    private String executionPhase;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "CANDIDATE";
        }
    }
}
