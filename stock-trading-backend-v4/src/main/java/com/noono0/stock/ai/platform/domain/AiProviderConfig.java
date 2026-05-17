package com.noono0.stock.ai.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_provider_config")
@Getter
@Setter
public class AiProviderConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_type", nullable = false, unique = true, length = 50)
    private String providerType;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "api_key_env_name", length = 100)
    private String apiKeyEnvName;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "default_weight", precision = 5, scale = 2)
    private BigDecimal defaultWeight = BigDecimal.ONE;

    @Column(name = "timeout_ms")
    private Integer timeoutMs = 30000;

    @Column(name = "max_retry_count")
    private Integer maxRetryCount = 1;

    @Column(name = "daily_budget_limit", precision = 12, scale = 2)
    private BigDecimal dailyBudgetLimit;

    @Column(name = "monthly_budget_limit", precision = 12, scale = 2)
    private BigDecimal monthlyBudgetLimit;

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
