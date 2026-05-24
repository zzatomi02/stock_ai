package com.noono0.stock.recommendation.dto;

import com.noono0.stock.recommendation.domain.StockRecommendation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record StockRecommendationDto(
        Long id,
        LocalDate tradeDate,
        String stockCode,
        String stockName,
        String status,
        BigDecimal bestAdjustedScore,
        String signalGrade,
        String strategyCodes,
        Long primaryStrategySignalId,
        String reasonSummary,
        boolean notificationSent,
        LocalDateTime notifiedAt,
        LocalDateTime approvedAt,
        String approvedBy,
        Long brokerOrderAttemptId,
        LocalDateTime createdAt) {

    public static StockRecommendationDto from(StockRecommendation r) {
        return new StockRecommendationDto(
                r.getId(),
                r.getTradeDate(),
                r.getStockCode(),
                r.getStockName(),
                r.getStatus(),
                r.getBestAdjustedScore(),
                r.getSignalGrade(),
                r.getStrategyCodes(),
                r.getPrimaryStrategySignalId(),
                r.getReasonSummary(),
                Boolean.TRUE.equals(r.getNotificationSent()),
                r.getNotifiedAt(),
                r.getApprovedAt(),
                r.getApprovedBy(),
                r.getBrokerOrderAttemptId(),
                r.getCreatedAt());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("tradeDate", tradeDate != null ? tradeDate.toString() : null);
        m.put("stockCode", stockCode);
        m.put("stockName", stockName);
        m.put("status", status);
        m.put("bestAdjustedScore", bestAdjustedScore);
        m.put("signalGrade", signalGrade);
        m.put("strategyCodes", strategyCodes);
        m.put("primaryStrategySignalId", primaryStrategySignalId);
        m.put("reasonSummary", reasonSummary);
        m.put("notificationSent", notificationSent);
        m.put("notifiedAt", notifiedAt != null ? notifiedAt.toString() : null);
        m.put("approvedAt", approvedAt != null ? approvedAt.toString() : null);
        m.put("approvedBy", approvedBy);
        m.put("brokerOrderAttemptId", brokerOrderAttemptId);
        m.put("createdAt", createdAt != null ? createdAt.toString() : null);
        return m;
    }
}
