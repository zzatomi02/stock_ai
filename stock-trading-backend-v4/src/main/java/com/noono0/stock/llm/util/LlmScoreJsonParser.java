package com.noono0.stock.llm.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM이 JSON(또는 JSON 블록)로 score를 내면 0~100 정수로 추출. 실패 시 50(중립).
 */
public final class LlmScoreJsonParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern RE_SCORE = Pattern.compile("\"score\"\\s*:\\s*(-?\\d+)");
    private static final Pattern RE_JSON_FENCE = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private LlmScoreJsonParser() {}

    public static int parseToScore0to100(String raw) {
        if (raw == null || raw.isBlank()) {
            return 50;
        }
        String text = raw.trim();
        String candidate = tryExtractFencedJson(text);
        if (candidate == null) {
            candidate = text;
        }
        Integer n = fromJsonString(candidate);
        if (n != null) {
            return n;
        }
        Matcher m = RE_SCORE.matcher(text);
        if (m.find()) {
            return clamp100(Integer.parseInt(m.group(1)));
        }
        return 50;
    }

    private static String tryExtractFencedJson(String text) {
        Matcher m = RE_JSON_FENCE.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private static Integer fromJsonString(String s) {
        if (s == null) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(s);
            if (root != null) {
                if (root.isObject()) {
                    if (root.has("score")) {
                        return clamp100(root.get("score").asInt(50));
                    }
                } else if (root.isInt()) {
                    return clamp100(root.asInt());
                }
            }
        } catch (Exception exception) {
            return null;
        }
        return null;
    }

    private static int clamp100(int n) {
        if (n < 0) {
            return 0;
        }
        if (n > 100) {
            return 100;
        }
        return n;
    }
}
