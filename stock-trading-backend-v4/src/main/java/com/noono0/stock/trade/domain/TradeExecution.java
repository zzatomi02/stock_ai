package com.noono0.stock.trade.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_execution")
@Getter
@Setter
public class TradeExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 120)
    private String stockName;

    @Column(name = "strategy_id")
    private Long strategyId;

    @Column(name = "mode", nullable = false, length = 20)
    private String mode;

    @Column(name = "bought_at", nullable = false)
    private LocalDateTime boughtAt;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @Column(name = "buy_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal buyPrice;

    @Column(name = "sell_price", precision = 19, scale = 4)
    private BigDecimal sellPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "profit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal profitAmount;

    @Column(name = "profit_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal profitRate;

    @Column(name = "news_score", nullable = false)
    private Integer newsScore;

    @Column(name = "ai_score", nullable = false)
    private Integer aiScore;

    @Column(nullable = false, length = 20)
    private String status;
}
