package com.noono0.stock.strategy.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.strategy.dto.MarketConditionStrategyUpdateRequest;
import com.noono0.stock.strategy.service.StrategySettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-condition-strategies")
@RequiredArgsConstructor
public class MarketConditionStrategyController {

    private final StrategySettingsService strategySettingsService;

    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(required = false) String marketCondition,
            @RequestParam(required = false) String strategyType) {
        return ApiResponse.ok(
                strategySettingsService.listMarketConditionStrategies(marketCondition, strategyType));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(
            @PathVariable long id, @Valid @RequestBody MarketConditionStrategyUpdateRequest body) {
        return ApiResponse.ok(
                strategySettingsService.updateMarketConditionStrategy(id, body),
                "시장별 전략 설정이 저장되었습니다.");
    }
}
