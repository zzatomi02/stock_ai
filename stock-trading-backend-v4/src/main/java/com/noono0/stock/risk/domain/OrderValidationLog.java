package com.noono0.stock.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_validation_log")
@Getter
@Setter
public class OrderValidationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(length = 8)
    private String side;

    @Column(name = "signal_id")
    private Long signalId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "validation_result", length = 20)
    private String validationResult;

    @Column(nullable = false)
    private Boolean passed;

    @Column(name = "execution_phase", length = 24)
    private String executionPhase;

    @Column(columnDefinition = "TEXT")
    private String checks;

    @Column(name = "validation_detail_json", columnDefinition = "TEXT")
    private String validationDetailJson;

    @Column(name = "fail_reason", columnDefinition = "TEXT")
    private String failReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
