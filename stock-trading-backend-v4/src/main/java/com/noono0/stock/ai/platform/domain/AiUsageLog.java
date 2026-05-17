package com.noono0.stock.ai.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_usage_log")
@Getter
@Setter
public class AiUsageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_type", length = 50)
    private String providerType;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "analysis_type", length = 100)
    private String analysisType;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "estimated_cost", precision = 12, scale = 4)
    private BigDecimal estimatedCost;

    @Column(name = "elapsed_ms")
    private Integer elapsedMs;

    @Column(name = "is_success", nullable = false)
    private boolean success;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
