package com.noono0.stock.strategy.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.strategy.dto.StrategyRuntimeSettingRequest;
import com.noono0.stock.strategy.service.StrategyConfigApprovalService;
import com.noono0.stock.strategy.service.StrategySettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping("/api/strategy-runtime-settings")
@RequiredArgsConstructor
public class StrategyRuntimeSettingController {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StrategySettingsService strategySettingsService;
    private final StrategyConfigApprovalService configApprovalService;

    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate tradeDate) {
        LocalDate d = tradeDate != null ? tradeDate : LocalDate.now(KST);
        return ApiResponse.ok(strategySettingsService.listRuntimeSettings(d));
    }

    @PostMapping
    public ApiResponse<?> save(
            @Valid @RequestBody StrategyRuntimeSettingRequest body,
            @RequestParam(defaultValue = "false") boolean requireApproval) {
        if (requireApproval) {
            var pending =
                    configApprovalService.submit(
                            body.strategyType(),
                            "RUNTIME_ENABLED",
                            "enabled",
                            String.valueOf(!body.isEnabled()),
                            String.valueOf(body.isEnabled()),
                            body.updatedBy() != null ? body.updatedBy() : "api");
            return ApiResponse.ok(
                    Map.of("pendingApprovalId", pending.getId(), "status", pending.getStatus()),
                    "관리자 승인 대기 중입니다.");
        }
        var saved = strategySettingsService.saveRuntimeSetting(body);
        return ApiResponse.ok(
                Map.of(
                        "id",
                        saved.getId(),
                        "tradeDate",
                        saved.getTradeDate().toString(),
                        "strategyType",
                        saved.getStrategyType(),
                        "isEnabled",
                        saved.getEnabled(),
                        "reason",
                        saved.getReason(),
                        "updatedBy",
                        saved.getUpdatedBy()),
                "오늘만 전략 ON/OFF가 저장되었습니다.");
    }
}
