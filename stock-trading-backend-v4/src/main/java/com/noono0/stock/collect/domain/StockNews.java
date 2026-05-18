package com.noono0.stock.collect.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_news",
        indexes = {
            @Index(name = "idx_stock_news_published_at", columnList = "published_at"),
            @Index(name = "idx_stock_news_collected_at", columnList = "collected_at"),
            @Index(name = "idx_stock_news_keyword_group", columnList = "keyword_group"),
            @Index(name = "idx_stock_news_related_stock_code", columnList = "related_stock_code")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_stock_news_duplicate_hash", columnNames = "duplicate_hash"))
@Getter
@Setter
public class StockNews {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "keyword_id")
    private Long keywordId;

    @Column(length = 200)
    private String keyword;

    @Column(name = "keyword_group", length = 50)
    private String keywordGroup;

    @Column(name = "search_type", length = 30)
    private String searchType;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "original_link", columnDefinition = "TEXT")
    private String originalLink;

    @Column(name = "naver_link", columnDefinition = "TEXT")
    private String naverLink;

    @Column(name = "press_name", length = 100)
    private String pressName;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "related_stock_code", length = 20)
    private String relatedStockCode;

    @Column(name = "related_stock_name", length = 100)
    private String relatedStockName;

    @Column(name = "related_theme", length = 100)
    private String relatedTheme;

    @Column(name = "sentiment_type", length = 20)
    private String sentimentType;

    @Column(name = "sentiment_score", precision = 6, scale = 2)
    private BigDecimal sentimentScore;

    @Column(name = "impact_score", precision = 6, scale = 2)
    private BigDecimal impactScore;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_reason", columnDefinition = "TEXT")
    private String aiReason;

    @Column(name = "ai_analyzed", nullable = false)
    private Boolean aiAnalyzed = false;

    @Column(name = "ai_analyzed_at")
    private LocalDateTime aiAnalyzedAt;

    @Column(name = "duplicate_hash", length = 100)
    private String duplicateHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (collectedAt == null) collectedAt = now;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (aiAnalyzed == null) aiAnalyzed = false;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
