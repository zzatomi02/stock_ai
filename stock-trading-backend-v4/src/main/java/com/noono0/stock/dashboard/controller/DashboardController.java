package com.noono0.stock.dashboard.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.dashboard.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public ApiResponse<?> dashboard(HttpServletRequest req) {
        return ApiResponse.ok(dashboardService.buildSummary(req));
    }
}
