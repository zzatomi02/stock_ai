package com.noono0.stock.market.dto;

import java.util.List;

public record StockDetailDto(String stockCode, String stockName, long price, double changeRate, long volume, List<CandleDto> dailyCandles, int newsScore, int aiScore, int finalScore, List<String> positiveKeywords) {}
