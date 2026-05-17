package com.noono0.stock.market.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "market_top100_snapshot")
@Getter
@Setter
public class MarketTop100Snapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String type;

    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @PrePersist
    void prePersist() {
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
}
