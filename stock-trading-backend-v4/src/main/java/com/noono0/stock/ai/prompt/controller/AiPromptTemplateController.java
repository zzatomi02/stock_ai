package com.noono0.stock.ai.prompt.controller;

import com.noono0.stock.ai.prompt.domain.AiPromptTemplate;
import com.noono0.stock.ai.prompt.service.AiPromptTemplateService;
import com.noono0.stock.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/prompts")
@RequiredArgsConstructor
public class AiPromptTemplateController {
    private final AiPromptTemplateService promptService;

    @GetMapping
    public ApiResponse<List<AiPromptTemplate>> list() {
        return ApiResponse.ok(promptService.listAll());
    }

    @GetMapping("/{promptCode}/versions")
    public ApiResponse<List<AiPromptTemplate>> versions(@PathVariable("promptCode") String promptCode) {
        return ApiResponse.ok(promptService.listVersions(promptCode));
    }

    @GetMapping("/active/{promptCode}")
    public ApiResponse<?> active(@PathVariable("promptCode") String promptCode) {
        return ApiResponse.ok(promptService.getActive(promptCode).orElse(null));
    }

    @PostMapping
    public ApiResponse<AiPromptTemplate> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(
                promptService.create(
                        str(body, "promptCode"),
                        str(body, "promptName"),
                        str(body, "promptType"),
                        str(body, "modelName"),
                        str(body, "systemPrompt"),
                        str(body, "userPrompt"),
                        str(body, "responseFormat"),
                        body.get("temperature") != null
                                ? new BigDecimal(String.valueOf(body.get("temperature")))
                                : null,
                        intOrNull(body, "maxTokens"),
                        str(body, "description")),
                "프롬프트 등록됨");
    }

    @PostMapping("/{id}/version")
    public ApiResponse<AiPromptTemplate> newVersion(
            @PathVariable("id") long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(
                promptService.createNewVersion(
                        id,
                        str(body, "promptName"),
                        str(body, "systemPrompt"),
                        str(body, "userPrompt"),
                        str(body, "responseFormat"),
                        body.get("temperature") != null
                                ? new BigDecimal(String.valueOf(body.get("temperature")))
                                : null,
                        intOrNull(body, "maxTokens"),
                        str(body, "description")),
                "새 버전 생성됨");
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<AiPromptTemplate> activate(@PathVariable("id") long id) {
        return ApiResponse.ok(promptService.setActive(id), "활성화됨");
    }

    @PostMapping("/{id}/active")
    public ApiResponse<?> setActive(@PathVariable("id") long id, @RequestParam("enabled") boolean enabled) {
        promptService.setActiveFlag(id, enabled);
        return ApiResponse.ok(Map.of("id", id, "active", enabled));
    }

    @PostMapping("/{id}/test")
    public ApiResponse<?> test(@PathVariable("id") long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(
                promptService.runTest(
                        id,
                        body.getOrDefault("stockCode", "005930"),
                        body.getOrDefault("stockName", "삼성전자"),
                        body.getOrDefault("title", ""),
                        body.getOrDefault("summary", "")));
    }

    @GetMapping("/{id}/test-logs")
    public ApiResponse<?> testLogs(@PathVariable("id") long id) {
        return ApiResponse.ok(promptService.testHistory(id));
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Integer intOrNull(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        return Integer.parseInt(String.valueOf(v));
    }
}
