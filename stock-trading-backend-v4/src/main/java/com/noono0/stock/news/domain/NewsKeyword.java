package com.noono0.stock.news.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_keyword")
@Getter
@Setter
public class NewsKeyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    /** POSITIVE | NEGATIVE | RISK */
    @Column(name = "keyword_type", nullable = false, length = 20)
    private String keywordType;

    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    private Integer weight = 10;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void pre() {
        LocalDateTime n = LocalDateTime.now();
        if (createdAt == null) createdAt = n;
        if (updatedAt == null) updatedAt = n;
        if (isActive == null) isActive = true;
        if (weight == null) weight = 10;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
