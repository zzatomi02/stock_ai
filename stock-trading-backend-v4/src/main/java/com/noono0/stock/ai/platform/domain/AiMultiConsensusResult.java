package com.noono0.stock.ai.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_multi_consensus_result")
@Getter
@Setter
public class AiMultiConsensusResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "analysis_type", length = 100)
    private String analysisType;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Column(name = "final_decision", length = 50)
    private String finalDecision;

    @Column(name = "final_score", precision = 6, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "final_confidence", precision = 5, scale = 2)
    private BigDecimal finalConfidence;

    @Column(name = "consensus_method", length = 50)
    private String consensusMethod;

    @Column(name = "provider_count")
    private Integer providerCount;

    @Column(name = "pass_count")
    private Integer passCount;

    @Column(name = "watch_count")
    private Integer watchCount;

    @Column(name = "avoid_count")
    private Integer avoidCount;

    @Column(name = "risk_count")
    private Integer riskCount;

    @Column(name = "disagreement_count")
    private Integer disagreementCount;

    @Column(name = "has_risk", nullable = false)
    private boolean hasRisk;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
