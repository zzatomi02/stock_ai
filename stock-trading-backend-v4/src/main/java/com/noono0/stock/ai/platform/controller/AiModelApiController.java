package com.noono0.stock.ai.platform.controller;

import com.noono0.stock.ai.platform.domain.AiModelConfig;
import com.noono0.stock.ai.platform.repository.AiModelConfigRepository;
import com.noono0.stock.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/models")
@RequiredArgsConstructor
public class AiModelApiController {

    private final AiModelConfigRepository repository;

    @GetMapping
    public ApiResponse<List<AiModelConfig>> list() {
        return ApiResponse.ok(repository.findAll());
    }

    @PutMapping("/{id}")
    public ApiResponse<AiModelConfig> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        AiModelConfig m =
                repository.findById(id).orElseThrow(() -> new IllegalArgumentException("model 없음"));
        if (body.containsKey("enabled")) {
            m.setEnabled(Boolean.TRUE.equals(body.get("enabled")));
        }
        if (body.get("modelName") != null) {
            m.setModelName(body.get("modelName").toString());
        }
        if (body.get("weight") != null) {
            m.setWeight(new java.math.BigDecimal(body.get("weight").toString()));
        }
        return ApiResponse.ok(repository.save(m));
    }
}
