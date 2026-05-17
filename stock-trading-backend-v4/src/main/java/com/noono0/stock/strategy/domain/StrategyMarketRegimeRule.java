package com.noono0.stock.strategy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "strategy_market_regime_rule",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_strategy_market",
                        columnNames = {"strategy_code", "market_regime"}))
@Getter
@Setter
public class StrategyMarketRegimeRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "strategy_code", nullable = false, length = 40)
    private String strategyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_regime", nullable = false, length = 20)
    private MarketRegime marketRegime;

    @Column(nullable = false)
    private Boolean enabled;

    /** 0~2.0 가중치 (시장별 전략 강도) */
    @Column(nullable = false)
    private Double weight;
}
