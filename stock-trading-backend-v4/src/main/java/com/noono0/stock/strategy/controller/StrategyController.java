package com.noono0.stock.strategy.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.strategy.service.StrategyService;
import com.noono0.stock.strategy.service.StrategySettingsService;
import com.noono0.stock.strategy.service.TradingClockService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/strategies")
@RequiredArgsConstructor
public class StrategyController {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StrategyService strategyService;
    private final StrategySettingsService strategySettingsService;
    private final TradingClockService tradingClock;

    @GetMapping
    public ApiResponse<?> findAll() {
        return ApiResponse.ok(strategyService.findAll());
    }

    @GetMapping("/enabled-now")
    public ApiResponse<?> enabledNow(
            @RequestParam(name = "evaluatedAt", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime evaluatedAt) {
        return ApiResponse.ok(withOptionalClock(evaluatedAt, strategySettingsService::getEnabledNow));
    }

    /** 8단계 판단 + 구성 점수 공식 (기사 없이 전략 게이트만) */
    @GetMapping("/decision-context")
    public ApiResponse<?> decisionContext(
            @RequestParam(name = "evaluatedAt", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime evaluatedAt) {
        return ApiResponse.ok(
                withOptionalClock(evaluatedAt, () -> strategySettingsService.getDecisionContext().toMap()));
    }

    private <T> T withOptionalClock(LocalDateTime evaluatedAt, java.util.function.Supplier<T> action) {
        if (evaluatedAt == null) {
            return action.get();
        }
        LocalDateTime kst = evaluatedAt.atZone(KST).toLocalDateTime();
        return tradingClock.runAt(kst, action);
    }
}
