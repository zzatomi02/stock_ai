package com.noono0.stock.news.dto;

public record NewsSourceUpsertRequest(
        String name,
        String baseUrl,
        String parserKey,
        Boolean enabled,
        Integer fetchIntervalSec,
        Integer maxArticlesPerRun,
        String requestHeadersJson,
        String selectorConfigJson
) {
}
