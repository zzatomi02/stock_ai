package com.noono0.stock.ai.platform.controller;

import com.noono0.stock.ai.platform.service.AiCompanyAnalysisQueryService;
import com.noono0.stock.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ai/company-analysis")
@RequiredArgsConstructor
public class AiCompanyAnalysisController {

    private final AiCompanyAnalysisQueryService queryService;

    @GetMapping("/today")
    public ApiResponse<?> today(
            @RequestParam(name = "stockCode", required = false) String stockCode,
            @RequestParam(name = "decision", required = false) String decision,
            @RequestParam(name = "providerType", required = false) String providerType,
            @RequestParam(name = "minScore", required = false) Double minScore) {
        return ApiResponse.ok(queryService.listToday(stockCode, decision, providerType, minScore));
    }

    @GetMapping("/{stockCode}")
    public ApiResponse<?> detail(
            @PathVariable String stockCode,
            @RequestParam(name = "tradeDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate tradeDate) {
        return ApiResponse.ok(queryService.detail(stockCode, tradeDate));
    }
}
