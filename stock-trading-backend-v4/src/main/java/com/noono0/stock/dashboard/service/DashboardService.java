package com.noono0.stock.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.noono0.stock.execution.OrderGateway;
import com.noono0.stock.integration.kis.service.KisBrokerService;
import com.noono0.stock.ops.trading.TradingControlService;
import com.noono0.stock.signal.service.TradingSignalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final KisBrokerService kisBrokerService;
    private final TradingControlService tradingControlService;
    private final TradingSignalService tradingSignalService;
    private final OrderGateway orderGateway;

    public Map<String, Object> buildSummary(HttpServletRequest req) {
        JsonNode balance = kisBrokerService.inquireBalance(req);
        boolean autoTrading = !tradingControlService.isEmergencyStop();

        long evaluationAmount = 0L;
        long todayProfitAmount = 0L;
        double todayProfitRate = 0d;
        String dataStatus = "LIVE";
        String dataReason = null;

        if (balance == null || balance.isMissingNode()) {
            dataStatus = "UNAVAILABLE";
            dataReason = "잔고 응답이 비어 있습니다.";
        } else if (balance.has("error")) {
            dataStatus = "UNAVAILABLE";
            dataReason = text(balance, "error");
        } else if (!"0".equals(text(balance, "rt_cd"))) {
            dataStatus = "UNAVAILABLE";
            dataReason = text(balance, "msg1");
            if (!StringUtils.hasText(dataReason)) {
                dataReason = "KIS 잔고 조회 실패(rt_cd=" + text(balance, "rt_cd") + ")";
            }
        } else {
            JsonNode summaryRow = firstRow(balance.path("output2"));
            evaluationAmount = parseLong(summaryRow,
                    List.of("tot_evlu_amt", "scts_evlu_amt", "tot_asst_amt"));
            todayProfitAmount = parseLong(summaryRow,
                    List.of("asst_icdc_amt", "evlu_pfls_smtl_amt", "evlu_pfls_amt_smtl_amt"));
            todayProfitRate = parseDouble(summaryRow,
                    List.of("asst_icdc_erng_rt", "tot_pftrt", "evlu_pfls_rt"));

            if (evaluationAmount == 0L && todayProfitAmount == 0L) {
                JsonNode holdings = balance.path("output1");
                if (holdings.isArray()) {
                    long totalEval = 0L;
                    long totalProfit = 0L;
                    for (JsonNode row : holdings) {
                        totalEval += parseLong(row, List.of("evlu_amt"));
                        totalProfit += parseLong(row, List.of("evlu_pfls_amt"));
                    }
                    evaluationAmount = totalEval;
                    todayProfitAmount = totalProfit;
                    long principal = totalEval - totalProfit;
                    if (principal > 0) {
                        todayProfitRate = (double) totalProfit * 100d / principal;
                    }
                }
            }
        }

        var buyCandidates = tradingSignalService.listToday("BUY", "CANDIDATE", 20);
        var sellWarnings = tradingSignalService.listToday("SELL", "CANDIDATE", 20);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("evaluationAmount", evaluationAmount);
        out.put("todayProfitAmount", todayProfitAmount);
        out.put("todayProfitRate", Math.round(todayProfitRate * 100d) / 100d);
        out.put("autoTrading", autoTrading);
        out.put("emergencyStop", tradingControlService.isEmergencyStop());
        out.put("executionPhase", orderGateway.currentPhase().name());
        out.put("observeOnly", orderGateway.allowsSignalOnly());
        out.put("buyCandidateCount", buyCandidates.size());
        out.put("sellWarningCount", sellWarnings.size());
        out.put("buyCandidates", buyCandidates.stream().limit(5).toList());
        out.put("sellWarnings", sellWarnings.stream().limit(5).toList());
        out.put("newsCollectionStatus", "UNAVAILABLE");
        out.put("topKeywords", new String[0]);
        out.put("marketSentimentScore", null);
        out.put("dataStatus", dataStatus);
        out.put("dataReason", dataReason == null ? "" : dataReason);
        return out;
    }

    private static JsonNode firstRow(JsonNode arr) {
        return arr.isArray() && !arr.isEmpty() ? arr.get(0) : null;
    }

    private static long parseLong(JsonNode node, List<String> fields) {
        if (node == null || node.isMissingNode()) return 0L;
        for (String f : fields) {
            String raw = text(node, f);
            if (!StringUtils.hasText(raw)) continue;
            String digits = raw.replaceAll("[^0-9\\-]", "");
            if (!StringUtils.hasText(digits) || "-".equals(digits)) continue;
            try {
                return Long.parseLong(digits);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0L;
    }

    private static double parseDouble(JsonNode node, List<String> fields) {
        if (node == null || node.isMissingNode()) return 0d;
        for (String f : fields) {
            String raw = text(node, f);
            if (!StringUtils.hasText(raw)) continue;
            String n = raw.replaceAll("[^0-9\\-.]", "");
            if (!StringUtils.hasText(n) || "-".equals(n) || ".".equals(n)) continue;
            try {
                return Double.parseDouble(n);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0d;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) return "";
        JsonNode v = node.path(field);
        return v.isMissingNode() ? "" : v.asText("").trim();
    }
}
