package com.noono0.stock.ai.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analysis_result")
@Getter
@Setter
public class AiAnalysisResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "provider_type", length = 50)
    private String providerType;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "analysis_type", length = 100)
    private String analysisType;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "parsed_json", columnDefinition = "JSON")
    private String parsedJson;

    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    @Column(name = "is_success", nullable = false)
    private boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "elapsed_ms")
    private Integer elapsedMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
