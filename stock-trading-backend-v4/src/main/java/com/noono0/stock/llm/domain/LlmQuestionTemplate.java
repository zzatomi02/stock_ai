package com.noono0.stock.llm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 뉴스(및 기타) LLM 질의용 시스템/사용자 메시지 템플릿.
 * <p>
 * userMessageTemplate 에는 {@code {{stockCode}}, {{stockName}}, {{title}}, {{summary}} } 등 치환 가능.
 */
@Entity
@Table(
        name = "llm_question_template",
        indexes = {
            @Index(name = "idx_llm_qt_default", columnList = "defaultTemplate"),
        })
@Getter
@Setter
public class LlmQuestionTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(name = "system_prompt", columnDefinition = "TEXT", nullable = false)
    private String systemPrompt;

    @Column(name = "user_message_template", columnDefinition = "TEXT", nullable = false)
    private String userMessageTemplate;

    @Column(name = "openai_model", nullable = false, length = 64)
    private String openaiModel = "gpt-4o-mini";

    @Column(name = "temperature", nullable = false)
    private double temperature = 0.2;

    /** true 인 행은 최대 1개 권장 — 서비스에서 단일 default 로 맞춤 */
    @Column(name = "default_template", nullable = false)
    private boolean defaultTemplate = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime n = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = n;
        }
        if (updatedAt == null) {
            updatedAt = n;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
