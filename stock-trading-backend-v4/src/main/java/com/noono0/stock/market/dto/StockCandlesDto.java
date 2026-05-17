package com.noono0.stock.market.dto;

import java.util.List;

/**
 * @param tf 요청에 사용한 봉 구분(예: 1m, 1h, 1d, 1w, 1mo)
 */
public record StockCandlesDto(String tf, List<CandleDto> candles) {}
