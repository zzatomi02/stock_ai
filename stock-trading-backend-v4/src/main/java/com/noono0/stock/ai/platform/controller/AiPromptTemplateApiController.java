package com.noono0.stock.ai.platform.controller;

import com.noono0.stock.ai.platform.domain.AiAnalysisPromptTemplate;
import com.noono0.stock.ai.platform.repository.AiAnalysisPromptTemplateRepository;
import com.noono0.stock.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/prompt-templates")
@RequiredArgsConstructor
public class AiPromptTemplateApiController {

    private final AiAnalysisPromptTemplateRepository repository;

    @GetMapping
    public ApiResponse<List<AiAnalysisPromptTemplate>> list() {
        return ApiResponse.ok(repository.findAll());
    }

    @PostMapping
    public ApiResponse<AiAnalysisPromptTemplate> create(@RequestBody AiAnalysisPromptTemplate body) {
        return ApiResponse.ok(repository.save(body));
    }

    @PutMapping("/{id}")
    public ApiResponse<AiAnalysisPromptTemplate> update(
            @PathVariable long id, @RequestBody AiAnalysisPromptTemplate body) {
        var existing =
                repository.findById(id).orElseThrow(() -> new IllegalArgumentException("템플릿 없음"));
        boolean contentChanged =
                !java.util.Objects.equals(existing.getUserPromptTemplate(), body.getUserPromptTemplate())
                        || !java.util.Objects.equals(existing.getSystemPrompt(), body.getSystemPrompt());
        if (contentChanged && body.getPromptVersion() == null) {
            body.setPromptVersion(bumpVersion(existing.getPromptVersion()));
        }
        body.setId(id);
        return ApiResponse.ok(repository.save(body));
    }

    private static String bumpVersion(String v) {
        if (v == null || v.isBlank()) {
            return "v1.1";
        }
        try {
            String num = v.replaceAll("[^0-9.]", "");
            double d = Double.parseDouble(num);
            return "v" + (d + 0.1);
        } catch (Exception e) {
            return v + ".1";
        }
    }
}
