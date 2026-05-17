package com.noono0.stock.ai.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_company_analysis",
        indexes = {
            @Index(name = "idx_aca_stock_date", columnList = "stock_code, trade_date"),
            @Index(name = "idx_aca_valid", columnList = "stock_code, valid_until")
        })
@Getter
@Setter
public class AiCompanyAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "stock_name", length = 100)
    private String stockName;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "provider_type", length = 50)
    private String providerType;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "overall_score", precision = 6, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "buy_score", precision = 6, scale = 2)
    private BigDecimal buyScore;

    @Column(name = "risk_score", precision = 6, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "news_score", precision = 6, scale = 2)
    private BigDecimal newsScore;

    @Column(name = "disclosure_score", precision = 6, scale = 2)
    private BigDecimal disclosureScore;

    @Column(name = "theme_score", precision = 6, scale = 2)
    private BigDecimal themeScore;

    @Column(name = "supply_score", precision = 6, scale = 2)
    private BigDecimal supplyScore;

    @Column(length = 50)
    private String decision;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "buy_reasons", columnDefinition = "JSON")
    private String buyReasons;

    @Column(name = "risk_reasons", columnDefinition = "JSON")
    private String riskReasons;

    @Column(name = "watch_points", columnDefinition = "JSON")
    private String watchPoints;

    @Column(name = "key_news_summary", columnDefinition = "TEXT")
    private String keyNewsSummary;

    @Column(name = "key_disclosure_summary", columnDefinition = "TEXT")
    private String keyDisclosureSummary;

    @Column(name = "theme_summary", columnDefinition = "TEXT")
    private String themeSummary;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
