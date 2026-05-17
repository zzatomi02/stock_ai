package com.noono0.stock.statistics.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TradeTimelineRow {
    private String tradeDate;
    private long tradeCount;
    private double profitAmount;
    private double avgProfitRate;
}
