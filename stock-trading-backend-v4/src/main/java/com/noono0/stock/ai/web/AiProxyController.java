package com.noono0.stock.ai.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.regex.Pattern;

/**
 * Python FastAPI 등 AI 서비스로 안전하게 프록시합니다. path는 화이트리스트 형식만 허용합니다.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiProxyController {

    private static final Pattern SAFE_PATH = Pattern.compile("^/[a-zA-Z0-9_./\\-]+$");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.base-url:http://localhost:8001}")
    private String baseUrl;

    @PostMapping("/forward")
    public ApiResponse<JsonNode> forwardPost(
            @RequestParam("path") String path, @RequestBody(required = false) JsonNode body)
            throws JsonProcessingException {
        return ApiResponse.ok(exchange("POST", path, body));
    }

    @GetMapping("/forward")
    public ApiResponse<JsonNode> forwardGet(@RequestParam("path") String path) throws JsonProcessingException {
        return ApiResponse.ok(exchange("GET", path, null));
    }

    private JsonNode exchange(String method, String path, JsonNode body) throws JsonProcessingException {
        validatePath(path);
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String url = base + path;
        String raw;
        if ("GET".equalsIgnoreCase(method)) {
            raw = restClient.get().uri(url).retrieve().body(String.class);
        } else if ("POST".equalsIgnoreCase(method)) {
            String payload = body != null ? objectMapper.writeValueAsString(body) : "{}";
            raw =
                    restClient
                            .post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .body(String.class);
        } else {
            throw new IllegalArgumentException("method");
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return objectMapper.createObjectNode().put("raw", raw);
        }
    }

    private void validatePath(String path) {
        if (!StringUtils.hasText(path) || !path.startsWith("/") || path.contains("..")) {
            throw new IllegalArgumentException("허용되지 않은 path입니다.");
        }
        if (!SAFE_PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("허용되지 않은 path 형식입니다.");
        }
    }
}
