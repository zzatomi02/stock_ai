package com.noono0.stock.ops.dashboard;

import com.noono0.stock.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops/dashboard")
@RequiredArgsConstructor
public class OpsDashboardController {

    private final OpsDashboardService dashboardService;

    @GetMapping
    public ApiResponse<?> snapshot(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ApiResponse.ok(dashboardService.snapshot(userId));
    }
}
