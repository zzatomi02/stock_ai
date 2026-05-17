package com.noono0.stock.strategy.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.strategy.dto.StrategyTimeWindowUpdateRequest;
import com.noono0.stock.strategy.service.StrategySettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/strategy-time-windows")
@RequiredArgsConstructor
public class StrategyTimeWindowController {

    private final StrategySettingsService strategySettingsService;

    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(required = false) String strategyType,
            @RequestParam(required = false) String marketTimeWindow) {
        return ApiResponse.ok(strategySettingsService.listStrategyTimeWindows(strategyType, marketTimeWindow));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable long id, @Valid @RequestBody StrategyTimeWindowUpdateRequest body) {
        return ApiResponse.ok(
                strategySettingsService.updateStrategyTimeWindow(id, body),
                "시간대별 전략 설정이 저장되었습니다.");
    }
}
