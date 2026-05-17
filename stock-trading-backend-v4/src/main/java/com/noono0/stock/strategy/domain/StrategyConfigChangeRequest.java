package com.noono0.stock.strategy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 전략 설정 변경 — 관리자 승인 후 반영 */
@Entity
@Table(name = "strategy_config_change_request")
@Getter
@Setter
public class StrategyConfigChangeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "strategy_type", nullable = false, length = 40)
    private String strategyType;

    @Column(name = "change_type", length = 50)
    private String changeType;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    /** PENDING | APPROVED | REJECTED */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
