package com.noono0.stock.ops.audit.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "audit_log",
        indexes = @Index(name = "idx_audit_created", columnList = "created_at"))
@Getter
@Setter
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;
    @Column(columnDefinition = "TEXT")
    private String detail;
    private Boolean success;
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (success == null) success = Boolean.TRUE;
    }
}
