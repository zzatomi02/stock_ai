package com.noono0.stock.ai.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_analysis_job",
        indexes = {
            @Index(name = "idx_aaj_status_sched", columnList = "status, scheduled_at"),
            @Index(name = "idx_aaj_stock", columnList = "stock_code, analysis_type")
        })
@Getter
@Setter
public class AiAnalysisJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "analysis_type", nullable = false, length = 100)
    private String analysisType;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "stock_name", length = 100)
    private String stockName;

    @Column(name = "provider_type", length = 50)
    private String providerType;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "prompt_template_id")
    private Long promptTemplateId;

    @Column(name = "execution_timing", length = 50)
    private String executionTiming;

    @Column(nullable = false, length = 50)
    private String status = "READY";

    private Integer priority = 0;

    @Column(name = "input_hash", length = 255)
    private String inputHash;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void pre() {
        LocalDateTime n = LocalDateTime.now();
        if (createdAt == null) createdAt = n;
        updatedAt = n;
        if (scheduledAt == null) scheduledAt = n;
    }

    @PreUpdate
    void preUp() {
        updatedAt = LocalDateTime.now();
    }
}
