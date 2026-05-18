package com.noono0.stock.collect.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "collect_error_log",
        indexes = {
            @Index(name = "idx_collect_error_log_created_at", columnList = "created_at"),
            @Index(name = "idx_collect_error_log_provider", columnList = "provider")
        })
@Getter
@Setter
public class CollectErrorLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_value", length = 300)
    private String targetValue;

    @Column(name = "error_type", length = 100)
    private String errorType;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "request_url", columnDefinition = "TEXT")
    private String requestUrl;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
