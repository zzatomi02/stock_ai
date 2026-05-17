package com.noono0.stock.strategy.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.strategy.dto.StrategyEnabledUpdateRequest;
import com.noono0.stock.strategy.service.StrategySettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/strategy-settings")
@RequiredArgsConstructor
public class StrategySettingsController {

    private final StrategySettingsService strategySettingsService;

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(strategySettingsService.listStrategySettings());
    }

    @PutMapping("/{strategyType}/enabled")
    public ApiResponse<?> updateEnabled(
            @PathVariable String strategyType, @Valid @RequestBody StrategyEnabledUpdateRequest body) {
        return ApiResponse.ok(
                strategySettingsService.updateMasterEnabled(strategyType, body.isEnabled()),
                "전략 기본 ON/OFF가 변경되었습니다.");
    }
}
