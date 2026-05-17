package com.noono0.stock.ai.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 분석용 질문 템플릿 (신규 스키마).
 *
 * <p>기존 {@code ai_prompt_template}(prompt_code 기반)과 별도 테이블 {@code ai_analysis_prompt_template}.
 */
@Entity
@Table(
        name = "ai_analysis_prompt_template",
        indexes = @Index(name = "idx_aapt_type_active", columnList = "analysis_type, is_active"))
@Getter
@Setter
public class AiAnalysisPromptTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "analysis_type", nullable = false, length = 100)
    private String analysisType;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "user_prompt_template", columnDefinition = "TEXT")
    private String userPromptTemplate;

    @Column(name = "response_schema_json", columnDefinition = "JSON")
    private String responseSchemaJson;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

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
