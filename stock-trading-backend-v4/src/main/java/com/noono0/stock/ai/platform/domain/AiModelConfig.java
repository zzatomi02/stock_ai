package com.noono0.stock.ai.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_model_config")
@Getter
@Setter
public class AiModelConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_type", nullable = false, length = 50)
    private String providerType;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "analysis_type", nullable = false, length = 100)
    private String analysisType;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight = BigDecimal.ONE;

    @Column(precision = 4, scale = 2)
    private BigDecimal temperature = new BigDecimal("0.20");

    @Column(name = "max_output_tokens")
    private Integer maxOutputTokens = 3000;

    @Column(name = "timeout_ms")
    private Integer timeoutMs = 30000;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void pre() {
        LocalDateTime n = LocalDateTime.now();
        if (createdAt == null) createdAt = n;
        updatedAt = n;
    }

    @PreUpdate
    void preUp() {
        updatedAt = LocalDateTime.now();
    }
}
