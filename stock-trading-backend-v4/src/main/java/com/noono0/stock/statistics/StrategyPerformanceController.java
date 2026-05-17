package com.noono0.stock.statistics;

import com.noono0.stock.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/strategy-performance")
@RequiredArgsConstructor
public class StrategyPerformanceController {

    private final StrategyPerformanceService performanceService;

    @GetMapping("/summary")
    public ApiResponse<?> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate f = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate t = to != null ? to : LocalDate.now();
        return ApiResponse.ok(performanceService.summary(f, t));
    }
}
