package com.noono0.stock.statistics.dto;

import java.util.List;

public record TradeStatisticsResponse(TradeSummaryRow summary, List<TradeTimelineRow> timeline, List<KeywordPerformanceRow> keywords) {}
