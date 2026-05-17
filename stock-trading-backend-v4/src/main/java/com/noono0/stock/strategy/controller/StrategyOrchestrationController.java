package com.noono0.stock.strategy.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.strategy.domain.enums.MarketCondition;
import com.noono0.stock.strategy.domain.enums.MarketTimeWindow;
import com.noono0.stock.strategy.service.StrategyOrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping("/api/strategies/orchestration")
@RequiredArgsConstructor
public class StrategyOrchestrationController {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StrategyOrchestrationService orchestrationService;

    @GetMapping("/snapshot")
    public ApiResponse<?> snapshot() {
        return ApiResponse.ok(orchestrationService.snapshotNow());
    }

    @PatchMapping("/{code}/enabled")
    public ApiResponse<?> setBaseEnabled(@PathVariable("code") String code, @RequestParam boolean enabled) {
        orchestrationService.setBaseEnabled(code, enabled);
        return ApiResponse.ok(Map.of("code", code, "enabled", enabled), "기본 ON/OFF가 변경되었습니다.");
    }

    @PutMapping("/{code}/today")
    public ApiResponse<?> setToday(
            @PathVariable("code") String code,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String memo) {
        LocalDate tradeDate = date != null ? date : LocalDate.now(KST);
        if (enabled == null) {
            orchestrationService.clearTodayOverride(code, tradeDate);
            return ApiResponse.ok(null, "오늘 임시 설정이 해제되었습니다.");
        }
        orchestrationService.setTodayOverride(code, tradeDate, enabled, memo);
        return ApiResponse.ok(
                Map.of("code", code, "tradeDate", tradeDate.toString(), "enabled", enabled),
                "오늘만 임시 ON/OFF가 저장되었습니다.");
    }

    @PutMapping("/{code}/market-rules")
    public ApiResponse<?> upsertMarketRule(
            @PathVariable("code") String code,
            @RequestParam MarketCondition marketCondition,
            @RequestParam boolean enabled,
            @RequestParam(defaultValue = "1.0") double weight) {
        orchestrationService.upsertMarketRule(code, marketCondition, enabled, weight);
        return ApiResponse.ok(orchestrationService.listMarketRules(code), "시장 상태 규칙이 저장되었습니다.");
    }

    @PutMapping("/{code}/time-rules")
    public ApiResponse<?> upsertTimeRule(
            @PathVariable("code") String code,
            @RequestParam MarketTimeWindow marketTimeWindow,
            @RequestParam boolean enabled,
            @RequestParam(defaultValue = "1.0") double weight) {
        orchestrationService.upsertTimeRule(code, marketTimeWindow, enabled, weight);
        return ApiResponse.ok(orchestrationService.listTimeRules(code), "시간대 규칙이 저장되었습니다.");
    }

    @GetMapping("/{code}/rules")
    public ApiResponse<?> rules(@PathVariable("code") String code) {
        return ApiResponse.ok(
                Map.of(
                        "marketRules", orchestrationService.listMarketRules(code),
                        "timeRules", orchestrationService.listTimeRules(code)));
    }
}
