package com.noono0.stock.recommendation.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.execution.runtime.ExecutionRuntimeConfig;
import com.noono0.stock.execution.runtime.ExecutionRuntimeService;
import com.noono0.stock.recommendation.service.StockRecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class StockRecommendationController {

    private final StockRecommendationService stockRecommendationService;
    private final ExecutionRuntimeService executionRuntimeService;

    @GetMapping("/stocks")
    public ApiResponse<?> list(@RequestParam(name = "status", required = false) String statusFilter) {
        var recommendations = stockRecommendationService.listToday(statusFilter);
        return ApiResponse.ok(
                Map.of(
                        "items",
                        recommendations.stream().map(dto -> dto.toMap()).toList(),
                        "phase",
                        executionRuntimeService.currentPhase().name()));
    }

    @GetMapping("/stocks/{id}")
    public ApiResponse<?> detail(@PathVariable("id") long recommendationId) {
        return ApiResponse.ok(stockRecommendationService.get(recommendationId).toMap());
    }

    @PostMapping("/sync")
    public ApiResponse<?> sync() {
        StockRecommendationService.SyncResult syncResult = stockRecommendationService.syncFromStrategySignals();
        return ApiResponse.ok(
                Map.of(
                        "created", syncResult.created(),
                        "updated", syncResult.updated(),
                        "notified", syncResult.notified(),
                        "message", syncResult.message()),
                "종목 추천 동기화");
    }

    @PostMapping("/stocks/{id}/approve")
    public ApiResponse<?> approve(@PathVariable("id") long recommendationId, HttpServletRequest request) {
        String userId = resolveUserId(request);
        return ApiResponse.ok(
                stockRecommendationService.approve(recommendationId, userId).toMap(),
                "승인·주문 요청 완료");
    }

    @PostMapping("/stocks/{id}/reject")
    public ApiResponse<?> reject(
            @PathVariable("id") long recommendationId,
            @RequestParam(name = "reason", required = false) String rejectReason,
            HttpServletRequest request) {
        String userId = resolveUserId(request);
        return ApiResponse.ok(
                stockRecommendationService.reject(recommendationId, userId, rejectReason).toMap(),
                "거절 처리");
    }

    @GetMapping("/settings")
    public ApiResponse<?> getSettings() {
        return ApiResponse.ok(buildSettingsResponse());
    }

    @PatchMapping("/settings")
    public ApiResponse<?> patchSettings(@RequestBody ExecutionRuntimeConfig incomingConfig) {
        executionRuntimeService.update(incomingConfig);
        return ApiResponse.ok(buildSettingsResponse(), "설정 저장");
    }

    private Map<String, Object> buildSettingsResponse() {
        ExecutionRuntimeConfig runtimeConfig = executionRuntimeService.get();
        var executionPhase = executionRuntimeService.currentPhase();
        return Map.ofEntries(
                Map.entry("phase", runtimeConfig.getPhase()),
                Map.entry("phaseLabel", executionPhase.labelKo()),
                Map.entry("orderQty", runtimeConfig.getOrderQty()),
                Map.entry("autoOrderScanEnabled", runtimeConfig.isAutoOrderScanEnabled()),
                Map.entry("autoOrderScanIntervalMs", runtimeConfig.getAutoOrderScanIntervalMs()),
                Map.entry("marketHoursOnly", runtimeConfig.isMarketHoursOnly()),
                Map.entry("realTradingEnabled", runtimeConfig.isRealTradingEnabled()),
                Map.entry("recommendation", runtimeConfig.getRecommendation()),
                Map.entry("notification", runtimeConfig.getNotification()),
                Map.entry("requiresApproval", executionPhase.requiresHumanApproval()),
                Map.entry("allowsAutoOrder", executionPhase.allowsAutoOrder()),
                Map.entry("observeOnly", executionPhase.isObserveOnly()));
    }

    private static String resolveUserId(HttpServletRequest request) {
        if (request == null) {
            return "system";
        }
        String headerUserId = request.getHeader("X-User-Id");
        return StringUtils.hasText(headerUserId) ? headerUserId.trim() : "system";
    }
}
