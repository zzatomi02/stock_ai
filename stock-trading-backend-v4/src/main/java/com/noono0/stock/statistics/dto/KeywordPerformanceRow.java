package com.noono0.stock.statistics.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class KeywordPerformanceRow {
    private String keyword;
    private long hitCount;
    private double avgProfitRate;
    private double totalProfitAmount;
}
