package com.noono0.stock.market.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.market.service.MarketMoodService;
import com.noono0.stock.market.service.MarketService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {
    private final MarketService marketService;
    private final MarketMoodService marketMoodService;

    @GetMapping("/mood")
    public ApiResponse<?> mood() {
        var s = marketMoodService.currentOrDefault();
        return ApiResponse.ok(
                Map.of(
                        "score", s.getMoodScore(),
                        "label", marketMoodService.labelForScore(s.getMoodScore()),
                        "snapshot", s));
    }

    @GetMapping("/top100")
    public ApiResponse<?> top100(
            @RequestParam(name = "type") String type, HttpServletRequest request) {
        return ApiResponse.ok(marketService.getTop100Snapshot(type, request));
    }

    @GetMapping("/stocks/{stockCode}")
    public ApiResponse<?> detail(
            @PathVariable("stockCode") String stockCode, HttpServletRequest request) {
        return ApiResponse.ok(marketService.getStockDetail(stockCode, request));
    }

    @GetMapping("/stocks/{stockCode}/candles")
    public ApiResponse<?> candles(
            @PathVariable("stockCode") String stockCode,
            @RequestParam(name = "tf") String tf,
            HttpServletRequest request) {
        return ApiResponse.ok(marketService.getStockCandles(stockCode, tf, request));
    }
}
