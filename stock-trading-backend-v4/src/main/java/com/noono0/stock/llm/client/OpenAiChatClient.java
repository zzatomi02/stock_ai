package com.noono0.stock.llm.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.noono0.stock.llm.config.LlmOpenAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * OpenAI Chat Completions (v1) 호출.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiChatClient {
    private final LlmOpenAiProperties props;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public String chatCompletions(
            String model, String systemMessage, String userMessage, double temperature) {
        if (!StringUtils.hasText(props.getApiKey())) {
            log.warn("【OPENAI-LLM】 API 키(app.openai.api-key) 없음 — 호출 생략");
            throw new IllegalStateException("OpenAI API 키가 설정되지 않았습니다. 환경 변수 OPENAI_API_KEY 또는 app.openai.api-key 를 설정하세요.");
        }
        if (!StringUtils.hasText(model)) {
            model = "gpt-4o-mini";
        }
        long t0 = System.currentTimeMillis();
        log.info(
                "【OPENAI-LLM】 ═══ chat 요청 ═══ model={} temp={} systemLen={} userLen={}",
                model,
                temperature,
                systemMessage == null ? 0 : systemMessage.length(),
                userMessage == null ? 0 : userMessage.length());
        if (log.isInfoEnabled() && systemMessage != null) {
            log.info("【OPENAI-LLM】   ↳ system(앞): {}", abbrev(systemMessage, 400));
        }
        if (log.isInfoEnabled() && userMessage != null) {
            log.info("【OPENAI-LLM】   ↳ user(앞): {}", abbrev(userMessage, 500));
        }

        String base = props.getBaseUrl() == null || props.getBaseUrl().isBlank()
                ? "https://api.openai.com"
                : props.getBaseUrl().replaceAll("/$", "");
        RestClient client =
                restClient
                        .mutate()
                        .baseUrl(base)
                        .defaultHeader("Authorization", "Bearer " + props.getApiKey().trim())
                        .build();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", temperature);
        ArrayNode messages = body.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemMessage == null ? "" : systemMessage);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userMessage == null ? "" : userMessage);

        String json;
        try {
            String payload = objectMapper.writeValueAsString(body);
            json =
                    client.post()
                            .uri("/v1/chat/completions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .body(String.class);
        } catch (RestClientResponseException e) {
            String errBody = e.getResponseBodyAsString();
            log.error("【OPENAI-LLM】 HTTP {} — {}", e.getStatusCode().value(), abbrev(errBody, 2000));
            throw new IllegalStateException("OpenAI 응답 오류: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("【OPENAI-LLM】 요청 실패: {}", e.getMessage());
            throw new IllegalStateException("OpenAI 호출 실패: " + e.getMessage(), e);
        }

        long ms = System.currentTimeMillis() - t0;
        try {
            JsonNode root = objectMapper.readTree(json);
            String text =
                    root.path("choices")
                            .path(0)
                            .path("message")
                            .path("content")
                            .asText(null);
            int pt = root.path("usage").path("prompt_tokens").asInt(-1);
            int ct = root.path("usage").path("completion_tokens").asInt(-1);
            log.info("【OPENAI-LLM】 ★ 응답 수신 ★ {} ms · prompt_tokens={} completion_tokens={} contentLen={}", ms, pt, ct, text == null ? 0 : text.length());
            log.info("【OPENAI-LLM】   ↳ content(앞): {}", abbrev(text, 800));
            if (text == null) {
                return "";
            }
            return text;
        } catch (Exception e) {
            log.error("【OPENAI-LLM】 응답 파싱 실패 body(앞)={}", abbrev(json, 1500), e);
            throw new IllegalStateException("OpenAI JSON 파싱 실패", e);
        }
    }

    private static String abbrev(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        if (max <= 3) {
            return s.substring(0, max);
        }
        return s.substring(0, max - 3) + "... [총 " + s.length() + "자]";
    }
}
