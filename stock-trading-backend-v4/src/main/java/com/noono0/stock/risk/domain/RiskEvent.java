package com.noono0.stock.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_event", indexes = @Index(name = "idx_risk_event_created", columnList = "created_at"))
@Getter
@Setter
public class RiskEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(length = 20)
    private String stockCode;

    @Column(name = "strategy_type", length = 40)
    private String strategyType;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
