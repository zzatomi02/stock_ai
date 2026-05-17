package com.noono0.stock.strategy.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.strategy.service.StrategyConfigApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/strategy-config-changes")
@RequiredArgsConstructor
public class StrategyConfigApprovalController {

    private final StrategyConfigApprovalService approvalService;

    @GetMapping("/pending")
    public ApiResponse<?> pending() {
        return ApiResponse.ok(approvalService.listPending());
    }

    @PostMapping
    public ApiResponse<?> submit(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(
                approvalService.submit(
                        body.get("strategyType"),
                        body.getOrDefault("changeType", "RUNTIME_ENABLED"),
                        body.get("fieldName"),
                        body.get("oldValue"),
                        body.get("newValue"),
                        body.getOrDefault("requestedBy", "admin")));
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<?> approve(@PathVariable long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(approvalService.approve(id, body.getOrDefault("approvedBy", "admin")));
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<?> reject(@PathVariable long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(
                approvalService.reject(id, body.getOrDefault("approvedBy", "admin"), body.get("reason")));
    }
}
