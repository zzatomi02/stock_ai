package com.noono0.stock.ai.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analysis_request")
@Getter
@Setter
public class AiAnalysisRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "provider_type", length = 50)
    private String providerType;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "analysis_type", length = 100)
    private String analysisType;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Column(name = "input_hash", length = 255)
    private String inputHash;

    @Column(name = "request_payload", columnDefinition = "JSON")
    private String requestPayload;

    @Column(name = "rendered_prompt", columnDefinition = "LONGTEXT")
    private String renderedPrompt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
