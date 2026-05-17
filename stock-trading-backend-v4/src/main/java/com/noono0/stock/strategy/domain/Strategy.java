package com.noono0.stock.strategy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "strategy")
@Getter
@Setter
public class Strategy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, length = 40)
    private String code;

    @Column(length = 20)
    private String category;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "target_market")
    private String targetMarket;

    @Column(name = "risk_limit_rate")
    private Double riskLimitRate;

    @Column(name = "take_profit_rate")
    private Double takeProfitRate;

    @Column(name = "max_buy_amount")
    private Long maxBuyAmount;

    @Column(name = "keyword_score_enabled")
    private Boolean keywordScoreEnabled;

    @Column(name = "ai_score_enabled")
    private Boolean aiScoreEnabled;

    @Column(name = "auto_trade_enabled")
    private Boolean autoTradeEnabled;

    /** 동일 종목 재진입 최소 간격(분) */
    @Column(name = "reentry_cooldown_minutes")
    private Integer reentryCooldownMinutes;

    /** adjusted 점수 최소 통과 기준 (매수형 기본 62, 매도형 38 등) */
    @Column(name = "min_score_threshold")
    private Integer minScoreThreshold;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
