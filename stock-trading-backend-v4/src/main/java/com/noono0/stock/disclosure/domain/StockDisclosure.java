package com.noono0.stock.disclosure.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_disclosure",
        indexes = {
            @Index(name = "idx_stock_disclosure_submitted_at", columnList = "submitted_at"),
            @Index(name = "idx_stock_disclosure_stock_code", columnList = "stock_code"),
            @Index(name = "idx_sd_stock_rcept", columnList = "stock_code, rcept_dt"),
            @Index(name = "idx_sd_collected", columnList = "collected_at")
        },
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_disclosure_rcept", columnNames = "rcept_no"),
            @UniqueConstraint(name = "uk_stock_disclosure_duplicate_hash", columnNames = "duplicate_hash")
        })
@Getter
@Setter
public class StockDisclosure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String provider = "DART";

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "stock_name", length = 100)
    private String stockName;

    @Column(name = "corp_code", length = 16)
    private String corpCode;

    @Column(name = "rcept_no", length = 100)
    private String rceptNo;

    @Column(name = "report_nm", nullable = false, length = 500)
    private String reportNm;

    @Column(name = "disclosure_type", length = 100)
    private String disclosureType;

    @Column(name = "original_url", columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "rcept_dt")
    private LocalDate rceptDt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "flr_nm", length = 200)
    private String flrNm;

    @Column(length = 500)
    private String rm;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_score", precision = 6, scale = 2)
    private java.math.BigDecimal aiScore;

    @Column(name = "risk_level", length = 30)
    private String riskLevel;

    @Column(name = "ai_analyzed", nullable = false)
    private Boolean aiAnalyzed = false;

    @Column(name = "duplicate_hash", length = 100)
    private String duplicateHash;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (collectedAt == null) collectedAt = LocalDateTime.now();
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (provider == null) provider = "DART";
        if (aiAnalyzed == null) aiAnalyzed = false;
        if (submittedAt == null && rceptDt != null) {
            submittedAt = rceptDt.atStartOfDay();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
