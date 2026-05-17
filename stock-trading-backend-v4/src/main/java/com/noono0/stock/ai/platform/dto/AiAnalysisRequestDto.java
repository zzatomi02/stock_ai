package com.noono0.stock.ai.platform.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AiAnalysisRequestDto(
        @NotBlank String analysisType,
        @NotBlank String stockCode,
        String stockName,
        List<String> providerTypes,
        Long promptTemplateId,
        String executionTiming) {}
