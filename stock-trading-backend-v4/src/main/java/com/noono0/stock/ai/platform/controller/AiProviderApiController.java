package com.noono0.stock.ai.platform.controller;

import com.noono0.stock.ai.platform.domain.AiProviderConfig;
import com.noono0.stock.ai.platform.dto.AiProviderConfigResponse;
import com.noono0.stock.ai.platform.repository.AiProviderConfigRepository;
import com.noono0.stock.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/providers")
@RequiredArgsConstructor
public class AiProviderApiController {

    private final AiProviderConfigRepository repository;

    @GetMapping
    public ApiResponse<List<AiProviderConfigResponse>> list() {
        return ApiResponse.ok(repository.findAll().stream().map(AiProviderConfigResponse::from).toList());
    }

    @PutMapping("/{providerType}")
    public ApiResponse<AiProviderConfigResponse> update(
            @PathVariable String providerType, @RequestBody Map<String, Object> body) {
        AiProviderConfig p =
                repository
                        .findByProviderType(providerType)
                        .orElseThrow(() -> new IllegalArgumentException("provider 없음"));
        if (body.containsKey("enabled")) {
            p.setEnabled(Boolean.TRUE.equals(body.get("enabled")));
        }
        if (body.get("defaultWeight") != null) {
            p.setDefaultWeight(new java.math.BigDecimal(body.get("defaultWeight").toString()));
        }
        if (body.get("description") != null) {
            p.setDescription(body.get("description").toString());
        }
        return ApiResponse.ok(AiProviderConfigResponse.from(repository.save(p)));
    }
}
