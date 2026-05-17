package com.noono0.stock.strategy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "strategy_daily_override",
        uniqueConstraints = @UniqueConstraint(name = "uk_strategy_daily", columnNames = {"strategy_code", "trade_date"}))
@Getter
@Setter
public class StrategyDailyOverride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "strategy_code", nullable = false, length = 40)
    private String strategyCode;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    /** null 이면 기본 설정 따름 */
    @Column(name = "enabled_override")
    private Boolean enabledOverride;

    @Column(length = 200)
    private String memo;
}
