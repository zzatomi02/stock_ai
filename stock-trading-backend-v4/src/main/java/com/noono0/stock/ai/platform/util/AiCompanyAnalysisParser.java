package com.noono0.stock.ai.platform.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.ai.platform.domain.AiCompanyAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AiCompanyAnalysisParser {
    private final ObjectMapper objectMapper;

    public String extractRawJson(String raw) {
        return extractJson(raw);
    }

    public AiCompanyAnalysis toEntity(
            String stockCode,
            String stockName,
            LocalDate tradeDate,
            String providerType,
            String modelName,
            String jsonText) throws Exception {
        JsonNode root = objectMapper.readTree(extractJson(jsonText));
        AiCompanyAnalysis a = new AiCompanyAnalysis();
        a.setStockCode(stockCode);
        a.setStockName(stockName);
        a.setTradeDate(tradeDate);
        a.setProviderType(providerType);
        a.setModelName(modelName);
        a.setOverallScore(dec(root, "overallScore"));
        a.setBuyScore(dec(root, "buyScore"));
        a.setRiskScore(dec(root, "riskScore"));
        a.setNewsScore(dec(root, "newsScore"));
        a.setDisclosureScore(dec(root, "disclosureScore"));
        a.setThemeScore(dec(root, "themeScore"));
        a.setSupplyScore(dec(root, "supplyScore"));
        a.setDecision(text(root, "decision"));
        a.setConfidence(dec(root, "confidence"));
        a.setSummary(text(root, "summary"));
        a.setBuyReasons(nodeToJson(root.get("buyReasons")));
        a.setRiskReasons(nodeToJson(root.get("riskReasons")));
        a.setWatchPoints(nodeToJson(root.get("watchPoints")));
        a.setKeyNewsSummary(text(root, "keyNewsSummary"));
        a.setKeyDisclosureSummary(text(root, "keyDisclosureSummary"));
        a.setThemeSummary(text(root, "themeSummary"));
        return a;
    }

    private static String extractJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private static BigDecimal dec(JsonNode n, String field) {
        if (n == null || !n.has(field) || n.get(field).isNull()) {
            return null;
        }
        return BigDecimal.valueOf(n.get(field).asDouble());
    }

    private static String text(JsonNode n, String field) {
        if (n == null || !n.has(field)) {
            return null;
        }
        return n.get(field).asText(null);
    }

    private String nodeToJson(JsonNode node) throws Exception {
        if (node == null || node.isNull()) {
            return "[]";
        }
        return objectMapper.writeValueAsString(node);
    }
}
