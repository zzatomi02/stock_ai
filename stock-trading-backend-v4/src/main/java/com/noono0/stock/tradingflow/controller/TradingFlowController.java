package com.noono0.stock.tradingflow.controller;

import com.noono0.stock.ai.platform.service.AiProviderPerformanceService;
import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.tradingflow.service.PostMarketPipelineService;
import com.noono0.stock.tradingflow.service.PreMarketPipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/trading-flow")
@RequiredArgsConstructor
public class TradingFlowController {

    private final PreMarketPipelineService preMarketPipelineService;
    private final PostMarketPipelineService postMarketPipelineService;
    private final AiProviderPerformanceService providerPerformanceService;

    @PostMapping("/pre-market/run")
    public ApiResponse<?> runPreMarket() {
        return ApiResponse.ok(preMarketPipelineService.run());
    }

    @PostMapping("/post-market/run")
    public ApiResponse<?> runPostMarket() {
        return ApiResponse.ok(postMarketPipelineService.run());
    }

    @GetMapping("/ai-provider-performance")
    public ApiResponse<?> aiPerformance(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(providerPerformanceService.statsSince(LocalDateTime.now().minusDays(days)));
    }
}
