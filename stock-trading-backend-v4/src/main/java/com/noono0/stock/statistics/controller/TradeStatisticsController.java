package com.noono0.stock.statistics.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.statistics.service.TradeStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class TradeStatisticsController {
    private final TradeStatisticsService tradeStatisticsService;

    @GetMapping("/trades")
    public ApiResponse<?> statistics(
            @RequestParam(name = "range") String range,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.ok(tradeStatisticsService.getStatistics(range, from, to, keyword));
    }

    @GetMapping("/health")
    public ApiResponse<?> health() {
        return ApiResponse.ok(tradeStatisticsService.healthSnapshot());
    }
}
