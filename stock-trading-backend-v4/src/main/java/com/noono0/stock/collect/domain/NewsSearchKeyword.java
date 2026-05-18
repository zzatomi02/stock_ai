package com.noono0.stock.collect.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 네이버 뉴스 검색 API용 키워드 (OR / SINGLE / STOCK) */
@Entity
@Table(name = "news_keyword")
@Getter
@Setter
public class NewsSearchKeyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String keyword;

    @Column(name = "keyword_group", nullable = false, length = 50)
    private String keywordGroup;

    @Column(name = "search_type", nullable = false, length = 30)
    private String searchType;

    @Column(nullable = false)
    private Integer priority = 5;

    @Column(name = "interval_seconds", nullable = false)
    private Integer intervalSeconds = 300;

    @Column(name = "display_count", nullable = false)
    private Integer displayCount = 10;

    @Column(name = "sort_type", nullable = false, length = 20)
    private String sortType = "date";

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(length = 500)
    private String description;

    @Column(name = "last_collected_at")
    private LocalDateTime lastCollectedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (priority == null) priority = 5;
        if (intervalSeconds == null) intervalSeconds = 300;
        if (displayCount == null) displayCount = 10;
        if (sortType == null) sortType = "date";
        if (enabled == null) enabled = true;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
