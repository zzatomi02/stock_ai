package com.noono0.stock.ai.platform.dto;

import com.noono0.stock.ai.platform.domain.AiProviderConfig;

import java.math.BigDecimal;

/** API Key는 환경변수명만 노출 (값 미노출) */
public record AiProviderConfigResponse(
        Long id,
        String providerType,
        String providerName,
        String baseUrl,
        String apiKeyEnvName,
        boolean enabled,
        BigDecimal defaultWeight,
        Integer timeoutMs,
        Integer maxRetryCount,
        String description) {

    public static AiProviderConfigResponse from(AiProviderConfig p) {
        return new AiProviderConfigResponse(
                p.getId(),
                p.getProviderType(),
                p.getProviderName(),
                p.getBaseUrl(),
                p.getApiKeyEnvName(),
                p.isEnabled(),
                p.getDefaultWeight(),
                p.getTimeoutMs(),
                p.getMaxRetryCount(),
                p.getDescription());
    }
}
