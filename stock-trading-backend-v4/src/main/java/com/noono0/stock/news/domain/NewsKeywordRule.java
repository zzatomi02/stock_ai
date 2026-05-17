package com.noono0.stock.news.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_keyword_rule")
@Getter
@Setter
public class NewsKeywordRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false, length = 20)
    private String polarity;

    @Column(nullable = false)
    private Boolean active;

    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = Boolean.TRUE;
        }
    }
}
