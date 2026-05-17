package com.noono0.stock.strategy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "strategy_time_slot_rule",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_strategy_time_slot",
                        columnNames = {"strategy_code", "time_slot_code"}))
@Getter
@Setter
public class StrategyTimeSlotRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "strategy_code", nullable = false, length = 40)
    private String strategyCode;

    @Column(name = "time_slot_code", nullable = false, length = 32)
    private String timeSlotCode;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false)
    private Double weight;

    /** true 이면 해당 슬롯에서 매수 주문 차단 */
    @Column(name = "block_buy", nullable = false)
    private Boolean blockBuy;
}
