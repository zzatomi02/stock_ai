package com.noono0.stock.news.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_keyword_match")
@Getter
@Setter
public class NewsKeywordMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "news_article_id", nullable = false)
    private Long newsArticleId;

    @Column(name = "keyword_id", nullable = false)
    private Long keywordId;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(name = "keyword_type", length = 20)
    private String keywordType;

    private Integer weight;

    @Column(name = "matched_text", columnDefinition = "TEXT")
    private String matchedText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
