package com.noono0.stock.statistics.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TradeSummaryRow {
    private long totalTrades;
    private long winCount;
    private long lossCount;
    private double avgProfitRate;
    private double totalProfitAmount;
    private double worstRate;
    private double bestRate;
}
