package com.noono0.stock.ai.platform.dto;

import com.noono0.stock.ai.platform.domain.AiCompanyAnalysis;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record CachedAiCompanyView(
        Long id,
        String stockCode,
        String stockName,
        String providerType,
        String modelName,
        BigDecimal overallScore,
        BigDecimal buyScore,
        BigDecimal riskScore,
        String decision,
        BigDecimal confidence,
        String summary,
        LocalDateTime validUntil) {

    public static CachedAiCompanyView from(AiCompanyAnalysis a) {
        return new CachedAiCompanyView(
                a.getId(),
                a.getStockCode(),
                a.getStockName(),
                a.getProviderType(),
                a.getModelName(),
                a.getOverallScore(),
                a.getBuyScore(),
                a.getRiskScore(),
                a.getDecision(),
                a.getConfidence(),
                a.getSummary(),
                a.getValidUntil());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("stockCode", stockCode);
        m.put("providerType", providerType);
        m.put("overallScore", overallScore);
        m.put("buyScore", buyScore);
        m.put("riskScore", riskScore);
        m.put("decision", decision);
        m.put("confidence", confidence);
        m.put("summary", summary);
        m.put("validUntil", validUntil != null ? validUntil.toString() : null);
        return m;
    }
}
