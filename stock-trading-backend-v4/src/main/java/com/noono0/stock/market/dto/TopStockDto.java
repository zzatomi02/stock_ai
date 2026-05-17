package com.noono0.stock.market.dto;

public record TopStockDto(int rank, String stockCode, String stockName, long price, double changeRate, long volume, int newsScore, int aiScore, int finalScore) {}
