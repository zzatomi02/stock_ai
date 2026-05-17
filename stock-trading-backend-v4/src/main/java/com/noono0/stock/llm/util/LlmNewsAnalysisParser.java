package com.noono0.stock.llm.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.llm.dto.LlmNewsAnalysisDto;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LlmNewsAnalysisParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern RE_JSON_FENCE =
            Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private LlmNewsAnalysisParser() {}

    public static LlmNewsAnalysisDto parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return LlmNewsAnalysisDto.empty();
        }
        String text = raw.trim();
        String candidate = extractJson(text);
        if (candidate == null) {
            candidate = text;
        }
        try {
            JsonNode root = MAPPER.readTree(candidate);
            if (root == null || !root.isObject()) {
                return legacyScoreOnly(raw);
            }
            return new LlmNewsAnalysisDto(
                    textOrNull(root, "stockCode"),
                    textOrNull(root, "stockName"),
                    normalizeSentiment(textOrNull(root, "sentiment")),
                    normalizeEventType(textOrNull(root, "eventType")),
                    intOrLegacyScore(root),
                    doubleOrDefault(root, "confidence", 0.5),
                    textOrNull(root, "summary"),
                    stringList(root, "reason"),
                    stringList(root, "risk"),
                    normalizeActionHint(textOrNull(root, "actionHint")));
        } catch (Exception e) {
            return legacyScoreOnly(raw);
        }
    }

    /** 하위 호환: score 필드만 있는 응답 */
    public static int parseImpactScore0to100(String raw) {
        return parse(raw).impactScore() != null ? parse(raw).impactScore() : 50;
    }

    private static LlmNewsAnalysisDto legacyScoreOnly(String raw) {
        int score = LlmScoreJsonParser.parseToScore0to100(raw);
        String hint = score >= 65 ? "WATCH_BUY" : score <= 35 ? "WATCH_SELL" : "HOLD";
        String sentiment = score >= 65 ? "POSITIVE" : score <= 35 ? "NEGATIVE" : "NEUTRAL";
        return new LlmNewsAnalysisDto(
                null, null, sentiment, "ETC", score, 0.5, null, List.of(), List.of(), hint);
    }

    private static String extractJson(String text) {
        Matcher m = RE_JSON_FENCE.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    private static Integer intOrLegacyScore(JsonNode root) {
        if (root.has("impactScore")) {
            return clamp100(root.get("impactScore").asInt(50));
        }
        if (root.has("score")) {
            return clamp100(root.get("score").asInt(50));
        }
        return 50;
    }

    private static double doubleOrDefault(JsonNode root, String key, double def) {
        if (!root.has(key)) {
            return def;
        }
        return root.get(key).asDouble(def);
    }

    private static String textOrNull(JsonNode root, String key) {
        if (!root.has(key) || root.get(key).isNull()) {
            return null;
        }
        return root.get(key).asText("").trim();
    }

    private static List<String> stringList(JsonNode root, String key) {
        if (!root.has(key) || !root.get(key).isArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (JsonNode n : root.get(key)) {
            if (n.isTextual() && !n.asText().isBlank()) {
                out.add(n.asText().trim());
            }
        }
        return out;
    }

    private static String normalizeSentiment(String s) {
        if (s == null) {
            return "NEUTRAL";
        }
        return switch (s.toUpperCase()) {
            case "POSITIVE", "NEGATIVE", "NEUTRAL" -> s.toUpperCase();
            default -> "NEUTRAL";
        };
    }

    private static String normalizeEventType(String s) {
        if (s == null || s.isBlank()) {
            return "ETC";
        }
        return s.toUpperCase();
    }

    private static String normalizeActionHint(String s) {
        if (s == null || s.isBlank()) {
            return "HOLD";
        }
        return switch (s.toUpperCase()) {
            case "WATCH_BUY", "WATCH_SELL", "HOLD", "IGNORE" -> s.toUpperCase();
            default -> "HOLD";
        };
    }

    private static int clamp100(int n) {
        return Math.max(0, Math.min(100, n));
    }
}
