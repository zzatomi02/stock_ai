package com.noono0.stock.collect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NewsSearchKeywordRequest(
        @NotBlank String keyword,
        @NotBlank String keywordGroup,
        @NotBlank String searchType,
        @NotNull Integer priority,
        @NotNull Integer intervalSeconds,
        @NotNull Integer displayCount,
        String sortType,
        Boolean enabled,
        String description) {}
