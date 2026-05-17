package com.noono0.stock.llm.dto;

import java.util.List;

/** GPT 뉴스 분석 JSON 스키마 */
public record LlmNewsAnalysisDto(
        String stockCode,
        String stockName,
        String sentiment,
        String eventType,
        Integer impactScore,
        Double confidence,
        String summary,
        List<String> reason,
        List<String> risk,
        String actionHint) {

    public static LlmNewsAnalysisDto empty() {
        return new LlmNewsAnalysisDto(null, null, "NEUTRAL", "ETC", 50, 0.5, null, List.of(), List.of(), "HOLD");
    }
}
