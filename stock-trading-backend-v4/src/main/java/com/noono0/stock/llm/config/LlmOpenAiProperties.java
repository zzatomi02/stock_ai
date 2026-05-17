package com.noono0.stock.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.openai")
public class LlmOpenAiProperties {
    /** 환경 변수 OPENAI_API_KEY 권장 */
    private String apiKey = "";
    /** 예: https://api.openai.com (슬래시 없이) */
    private String baseUrl = "https://api.openai.com";
    /** true면 뉴스 ingest 직후 기본 템플릿으로 LLM 분석(비용·지연 있음) */
    private boolean analyzeOnIngest = false;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isAnalyzeOnIngest() {
        return analyzeOnIngest;
    }

    public void setAnalyzeOnIngest(boolean analyzeOnIngest) {
        this.analyzeOnIngest = analyzeOnIngest;
    }
}
