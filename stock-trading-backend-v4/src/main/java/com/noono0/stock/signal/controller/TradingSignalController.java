package com.noono0.stock.signal.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.signal.domain.TradingSignal;
import com.noono0.stock.signal.service.TradingSignalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class TradingSignalController {
    private final TradingSignalService tradingSignalService;

    @GetMapping("/platform")
    public ApiResponse<?> platform() {
        return ApiResponse.ok(tradingSignalService.platformStatus());
    }

    @GetMapping
    public ApiResponse<List<TradingSignal>> list(
            @RequestParam(name = "side", required = false) String side,
            @RequestParam(name = "status", defaultValue = "CANDIDATE") String status,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return ApiResponse.ok(tradingSignalService.listToday(side, status, limit));
    }

    @GetMapping("/rejected")
    public ApiResponse<List<TradingSignal>> rejected(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return ApiResponse.ok(tradingSignalService.listRejected(limit));
    }

    @GetMapping("/grades/{grade}")
    public ApiResponse<List<TradingSignal>> byGrade(
            @PathVariable("grade") String grade, @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return ApiResponse.ok(tradingSignalService.listByGrade(grade.toUpperCase(), limit));
    }

    @PostMapping("/generate/{articleId}")
    public ApiResponse<?> generate(
            @PathVariable("articleId") long articleId, HttpServletRequest req) {
        String userId = req.getHeader("X-User-Id");
        return ApiResponse.ok(
                tradingSignalService
                        .generateFromArticleId(articleId, userId)
                        .orElse(null),
                "시그널 생성 시도 완료");
    }

    @PostMapping("/scan-recent")
    public ApiResponse<?> scanRecent(@RequestParam(name = "hours", defaultValue = "24") int hours) {
        int n = tradingSignalService.scanRecentArticles(hours);
        return ApiResponse.ok(Map.of("created", n), "최근 기사 스캔 완료");
    }
}
