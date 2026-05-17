package com.noono0.stock.disclosure.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_disclosure",
        uniqueConstraints = @UniqueConstraint(name = "uk_disclosure_rcept", columnNames = "rcept_no"),
        indexes = {
            @Index(name = "idx_sd_stock_rcept", columnList = "stock_code, rcept_dt"),
            @Index(name = "idx_sd_collected", columnList = "collected_at")
        })
@Getter
@Setter
public class StockDisclosure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "corp_code", nullable = false, length = 16)
    private String corpCode;

    @Column(name = "rcept_no", nullable = false, length = 32)
    private String rceptNo;

    @Column(name = "report_nm", nullable = false, length = 500)
    private String reportNm;

    @Column(name = "rcept_dt", nullable = false)
    private LocalDate rceptDt;

    @Column(name = "flr_nm", length = 200)
    private String flrNm;

    @Column(length = 500)
    private String rm;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;
}
