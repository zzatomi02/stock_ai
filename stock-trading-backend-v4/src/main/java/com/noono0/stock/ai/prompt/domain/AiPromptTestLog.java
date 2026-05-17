package com.noono0.stock.ai.prompt.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_prompt_test_log")
@Getter
@Setter
public class AiPromptTestLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "prompt_code", length = 100)
    private String promptCode;

    @Column(name = "input_json", columnDefinition = "TEXT")
    private String inputJson;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "parsed_score")
    private Integer parsedScore;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
