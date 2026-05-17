package com.noono0.stock.broker.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "broker_order_attempt",
        indexes = @Index(name = "idx_broker_stock_time", columnList = "stock_code, created_at"))
@Getter
@Setter
public class BrokerOrderAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_order_key", nullable = false, unique = true, length = 160)
    private String clientOrderKey;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(nullable = false, length = 12)
    private String side;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 20)
    private String mode;
    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    /** 주문/자동매매 사유(기술·뉴스·전략 요약 JSON 텍스트 등) */
    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "signal_id")
    private Long signalId;

    @Column(name = "reason_snapshot", columnDefinition = "TEXT")
    private String reasonSnapshot;

    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
