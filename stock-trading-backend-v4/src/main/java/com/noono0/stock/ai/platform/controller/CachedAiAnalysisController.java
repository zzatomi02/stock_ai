package com.noono0.stock.ai.platform.controller;

import com.noono0.stock.ai.platform.service.CachedAiAnalysisQueryService;
import com.noono0.stock.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 장중 매매 판단용 — 저장된 AI 결과만 조회 (외부 API 호출 없음).
 */
@RestController
@RequestMapping("/api/ai/cached")
@RequiredArgsConstructor
public class CachedAiAnalysisController {

    private final CachedAiAnalysisQueryService queryService;

    @GetMapping("/company/{stockCode}")
    public ApiResponse<?> best(@PathVariable String stockCode) {
        return ApiResponse.ok(
                queryService
                        .getBestForTrading(stockCode)
                        .map(v -> v.toMap())
                        .orElse(null));
    }

    @GetMapping("/company/{stockCode}/bundle")
    public ApiResponse<?> bundle(
            @PathVariable String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate) {
        LocalDate d = tradeDate != null ? tradeDate : LocalDate.now();
        return ApiResponse.ok(queryService.getTradingBundle(stockCode, d));
    }
}
