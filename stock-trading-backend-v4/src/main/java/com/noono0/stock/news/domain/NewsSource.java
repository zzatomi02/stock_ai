package com.noono0.stock.news.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_source")
@Getter
@Setter
public class NewsSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "parser_key", nullable = false, length = 60)
    private String parserKey;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "fetch_interval_sec", nullable = false)
    private Integer fetchIntervalSec = 300;

    @Column(name = "max_articles_per_run", nullable = false)
    private Integer maxArticlesPerRun = 30;

    @Column(name = "request_headers_json", columnDefinition = "TEXT")
    private String requestHeadersJson;

    @Column(name = "selector_config_json", columnDefinition = "TEXT")
    private String selectorConfigJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (enabled == null) enabled = true;
        if (fetchIntervalSec == null || fetchIntervalSec <= 0) fetchIntervalSec = 300;
        if (maxArticlesPerRun == null || maxArticlesPerRun <= 0) maxArticlesPerRun = 30;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (enabled == null) enabled = true;
        if (fetchIntervalSec == null || fetchIntervalSec <= 0) fetchIntervalSec = 300;
        if (maxArticlesPerRun == null || maxArticlesPerRun <= 0) maxArticlesPerRun = 30;
    }
}
