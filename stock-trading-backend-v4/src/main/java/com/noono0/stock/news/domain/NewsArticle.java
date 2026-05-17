package com.noono0.stock.news.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "news_article",
        uniqueConstraints = @UniqueConstraint(name = "uk_news_dedup", columnNames = "dedup_hash"))
@Getter
@Setter
public class NewsArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "source_name", length = 120)
    private String sourceName;

    @Column(name = "article_url", length = 500)
    private String articleUrl;

    @Column(name = "full_body", columnDefinition = "LONGTEXT")
    private String fullBody;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "keyword_score", nullable = false)
    private Integer keywordScore;

    @Column(name = "ai_score", nullable = false)
    private Integer aiScore;

    /** SHA-256 등 중복 판별용 (nullable: 기존 행) */
    @Column(name = "dedup_hash", length = 64)
    private String dedupHash;

    /** OpenAI 등 LLM 원문(점수·JSON) — 감사·디버깅 */
    @Column(name = "llm_raw_response", columnDefinition = "TEXT")
    private String llmRawResponse;

    @Column(name = "llm_model", length = 64)
    private String llmModel;

    @Column(name = "llm_template_name", length = 120)
    private String llmTemplateName;

    @Column(name = "llm_analyzed_at")
    private LocalDateTime llmAnalyzedAt;

    @Column(name = "llm_stock_name", length = 120)
    private String llmStockName;

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

    @Column(name = "llm_reason_json", columnDefinition = "TEXT")
    private String llmReasonJson;

    @Column(name = "llm_risk_json", columnDefinition = "TEXT")
    private String llmRiskJson;

    @PrePersist
    void prePersist() {
        if (collectedAt == null) {
            collectedAt = LocalDateTime.now();
        }
        if (keywordScore == null) {
            keywordScore = 0;
        }
        if (aiScore == null) {
            aiScore = 50;
        }
    }
}
