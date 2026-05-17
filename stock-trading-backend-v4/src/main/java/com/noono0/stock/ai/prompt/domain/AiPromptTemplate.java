package com.noono0.stock.ai.prompt.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_prompt_template",
        uniqueConstraints = @UniqueConstraint(name = "uk_prompt_code_version", columnNames = {"prompt_code", "version"}))
@Getter
@Setter
public class AiPromptTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prompt_code", nullable = false, length = 100)
    private String promptCode;

    @Column(name = "prompt_name", nullable = false, length = 200)
    private String promptName;

    @Column(name = "prompt_type", nullable = false, length = 50)
    private String promptType;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "user_prompt", columnDefinition = "TEXT")
    private String userPrompt;

    @Column(name = "response_format", columnDefinition = "TEXT")
    private String responseFormat;

    @Column(precision = 3, scale = 2)
    private BigDecimal temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime n = LocalDateTime.now();
        if (createdAt == null) createdAt = n;
        if (updatedAt == null) updatedAt = n;
        if (isActive == null) isActive = true;
        if (version == null) version = 1;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
