package com.noono0.stock.market.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "market_mood_snapshot")
@Getter
@Setter
public class MarketMoodSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mood_score", nullable = false)
    private Integer moodScore;

    @Column(name = "mood_label", length = 32)
    private String moodLabel;

    @Column(name = "kospi_change_rate")
    private Double kospiChangeRate;

    @Column(name = "kosdaq_change_rate")
    private Double kosdaqChangeRate;

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
