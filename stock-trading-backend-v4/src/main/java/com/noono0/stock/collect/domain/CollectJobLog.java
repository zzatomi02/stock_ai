package com.noono0.stock.collect.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "collect_job_log",
        indexes = {
            @Index(name = "idx_collect_job_log_started_at", columnList = "started_at"),
            @Index(name = "idx_collect_job_log_provider", columnList = "provider"),
            @Index(name = "idx_collect_job_log_status", columnList = "status")
        })
@Getter
@Setter
public class CollectJobLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_value", length = 300)
    private String targetValue;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "request_count", nullable = false)
    private Integer requestCount = 0;

    @Column(name = "response_count", nullable = false)
    private Integer responseCount = 0;

    @Column(name = "saved_count", nullable = false)
    private Integer savedCount = 0;

    @Column(name = "duplicate_count", nullable = false)
    private Integer duplicateCount = 0;

    @Column(name = "skipped_count", nullable = false)
    private Integer skippedCount = 0;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount = 0;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
