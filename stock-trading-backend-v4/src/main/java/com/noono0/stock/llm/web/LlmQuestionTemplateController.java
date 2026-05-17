package com.noono0.stock.llm.web;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.llm.domain.LlmQuestionTemplate;
import com.noono0.stock.llm.service.LlmQuestionTemplateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/api/llm")
@RestController
@RequiredArgsConstructor
public class LlmQuestionTemplateController {
    private final LlmQuestionTemplateService templateService;

    @GetMapping("/question-templates")
    public ApiResponse<?> list() {
        List<LlmQuestionTemplate> rows = templateService.list();
        return ApiResponse.ok(
                rows.stream()
                        .map(t -> {
                            Map<String, Object> m = new HashMap<>();
                            m.put("id", t.getId());
                            m.put("name", t.getName());
                            m.put("systemPrompt", t.getSystemPrompt());
                            m.put("userMessageTemplate", t.getUserMessageTemplate());
                            m.put("openaiModel", t.getOpenaiModel());
                            m.put("temperature", t.getTemperature());
                            m.put("defaultTemplate", t.isDefaultTemplate());
                            m.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
                            m.put("updatedAt", t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null);
                            return m;
                        })
                        .toList());
    }

    public record CreateBody(
            @NotBlank String name,
            @NotBlank String systemPrompt,
            @NotBlank String userMessageTemplate,
            String openaiModel,
            Double temperature) {}

    @PostMapping("/question-templates")
    public ApiResponse<?> create(@Valid @RequestBody CreateBody b) {
        LlmQuestionTemplate t =
                templateService.create(
                        b.name(), b.systemPrompt(), b.userMessageTemplate(), b.openaiModel(), b.temperature());
        return ApiResponse.ok(Map.of("id", t.getId()));
    }

    public record UpdateBody(
            String name, String systemPrompt, String userMessageTemplate, String openaiModel, Double temperature) {}

    @PutMapping("/question-templates/{id}")
    public ApiResponse<?> update(
            @PathVariable("id") long id, @Valid @RequestBody UpdateBody b) {
        LlmQuestionTemplate t =
                templateService.update(
                        id,
                        b.name(),
                        b.systemPrompt(),
                        b.userMessageTemplate(),
                        b.openaiModel(),
                        b.temperature());
        return ApiResponse.ok(Map.of("id", t.getId()));
    }

    @DeleteMapping("/question-templates/{id}")
    public ApiResponse<?> delete(@PathVariable("id") long id) {
        templateService.delete(id);
        return ApiResponse.ok(Map.of("deleted", true, "id", id));
    }

    @PostMapping("/question-templates/{id}/set-default")
    public ApiResponse<?> setDefault(@PathVariable("id") long id) {
        templateService.setAsDefault(id);
        return ApiResponse.ok(Map.of("id", id, "defaultTemplate", true));
    }

    public record PreviewBody(
            Long templateId,
            String stockCode,
            String stockName,
            @NotBlank String title,
            String summary) {}

    @PostMapping("/question-templates/preview")
    public ApiResponse<?> preview(@Valid @RequestBody PreviewBody b) {
        LlmQuestionTemplateService.PreviewResult r;
        if (b.templateId() != null) {
            r = templateService.preview(b.templateId(), b.stockCode(), b.stockName(), b.title(), b.summary());
        } else {
            r = templateService.previewWithDefault(b.stockCode(), b.stockName(), b.title(), b.summary());
        }
        return ApiResponse.ok(
                Map.of(
                        "templateId", r.templateId(),
                        "templateName", r.templateName(),
                        "model", r.model(),
                        "rawContent", r.rawLlm(),
                        "parsedScore", r.parsedScore()));
    }
}
