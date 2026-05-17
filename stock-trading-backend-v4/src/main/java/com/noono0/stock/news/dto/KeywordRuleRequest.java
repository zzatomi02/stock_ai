package com.noono0.stock.news.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KeywordRuleRequest(@NotBlank String keyword, @NotNull Integer score, @NotBlank String polarity, String description) {}
