package com.noono0.stock.strategy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 전략별 분석 결과 (시장·시간 가중치 적용 raw/adjusted 점수) */
@Entity
@Table(
        name = "strategy_signal",
        indexes = {
            @Index(name = "idx_strategy_signal_article", columnList = "news_article_id, strategy_code"),
            @Index(name = "idx_strategy_signal_created", columnList = "created_at")
        })
@Getter
@Setter
public class StrategySignal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "strategy_code", nullable = false, length = 40)
    private String strategyCode;

    @Column(name = "news_article_id")
    private Long newsArticleId;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "stock_name", length = 120)
    private String stockName;

    /** BUY | SELL | ANALYSIS */
    @Column(length = 12)
    private String side;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "market_condition", length = 50)
    private String marketCondition;

    @Column(name = "market_time_window", length = 50)
    private String marketTimeWindow;

    /** @deprecated market_condition 사용 */
    @Column(name = "time_slot_code", length = 32)
    private String timeSlotCode;

    /** @deprecated market_condition 사용 */
    @Column(name = "market_regime", length = 20)
    private String marketRegime;

    @Column(name = "raw_final_score", precision = 5, scale = 2)
    private BigDecimal rawFinalScore;

    @Column(name = "adjusted_final_score", precision = 5, scale = 2)
    private BigDecimal adjustedFinalScore;

    @Column(name = "market_weight_multiplier", precision = 5, scale = 2)
    private BigDecimal marketWeightMultiplier;

    @Column(name = "time_weight_multiplier", precision = 5, scale = 2)
    private BigDecimal timeWeightMultiplier;

    @Column(name = "final_weight_multiplier", precision = 5, scale = 2)
    private BigDecimal finalWeightMultiplier;

    /** @deprecated market_weight_multiplier 사용 */
    @Column(name = "market_weight")
    private Double marketWeight;

    /** @deprecated time_weight_multiplier 사용 */
    @Column(name = "time_weight")
    private Double timeWeight;

    @Column(name = "min_score_threshold", nullable = false)
    private Integer minScoreThreshold;

    @Column(name = "signal_grade", length = 2)
    private String signalGrade;

    /** CANDIDATE | REJECTED | EXCLUDED */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "strategy_selected_reason", length = 1000)
    private String strategySelectedReason;

    @Column(name = "strategy_excluded_reason", length = 1000)
    private String strategyExcludedReason;

    @Column(name = "reason_message", length = 2000)
    private String reasonMessage;

    @Column(name = "execution_log", columnDefinition = "TEXT")
    private String executionLog;

    @Column(name = "rule_based_score")
    private Integer ruleBasedScore;

    @Column(name = "ai_overall_score", precision = 5, scale = 2)
    private BigDecimal aiOverallScore;

    @Column(name = "ai_buy_score", precision = 5, scale = 2)
    private BigDecimal aiBuyScore;

    @Column(name = "ai_risk_score", precision = 5, scale = 2)
    private BigDecimal aiRiskScore;

    @Column(name = "ai_blended_score", precision = 5, scale = 2)
    private BigDecimal aiBlendedScore;

    @Column(name = "ai_decision", length = 50)
    private String aiDecision;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_company_analysis_id")
    private Long aiCompanyAnalysisId;

    @Column(name = "risk_check_passed")
    private Boolean riskCheckPassed;

    @Column(name = "risk_block_reason", length = 500)
    private String riskBlockReason;

    @Column(name = "prompt_version_used", length = 50)
    private String promptVersionUsed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        syncLegacyColumns();
    }

    @PreUpdate
    void preUpdate() {
        syncLegacyColumns();
    }

    private void syncLegacyColumns() {
        if (marketRegime == null && marketCondition != null) {
            marketRegime = marketCondition;
        }
        if (timeSlotCode == null && marketTimeWindow != null) {
            timeSlotCode = marketTimeWindow;
        }
        if (marketWeight == null && marketWeightMultiplier != null) {
            marketWeight = marketWeightMultiplier.doubleValue();
        }
        if (timeWeight == null && timeWeightMultiplier != null) {
            timeWeight = timeWeightMultiplier.doubleValue();
        }
    }
}
