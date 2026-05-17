package com.noono0.stock.strategy.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.strategy.service.StrategyAnalysisService;
import com.noono0.stock.strategy.service.TradingClockService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping("/api/strategies/analysis")
@RequiredArgsConstructor
public class StrategyAnalysisController {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StrategyAnalysisService strategyAnalysisService;
    @PostMapping("/article/{articleId}")
    public ApiResponse<?> analyzeArticle(
            @PathVariable("articleId") long articleId,
            @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime evaluatedAt) {
        LocalDateTime at = evaluatedAt != null ? evaluatedAt.atZone(KST).toLocalDateTime() : LocalDateTime.now(KST);
        var results = strategyAnalysisService.analyzeArticle(articleId, at);
        long candidates = results.stream().filter(r -> "CANDIDATE".equals(r.status())).count();
        return ApiResponse.ok(
                Map.of(
                        "articleId",
                        articleId,
                        "candidateCount",
                        candidates,
                        "results",
                        results.stream().map(r -> r.toMap()).toList()),
                "전략별 분석 완료");
    }

    @PostMapping("/scan-recent")
    public ApiResponse<?> scanRecent(
            @RequestParam(name = "hours", defaultValue = "24") int hours,
            @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime evaluatedAt) {
        LocalDateTime at = evaluatedAt != null ? evaluatedAt.atZone(KST).toLocalDateTime() : LocalDateTime.now(KST);
        return ApiResponse.ok(
                strategyAnalysisService.analyzeRecentArticles(hours, at),
                "최근 기사 전략 분석 완료");
    }

    @GetMapping("/signals")
    public ApiResponse<?> signals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "status", defaultValue = "CANDIDATE") String status) {
        LocalDate d = date != null ? date : LocalDate.now(KST);
        var list = strategyAnalysisService.listSignals(d, status);
        return ApiResponse.ok(
                Map.of(
                        "tradeDate",
                        d.toString(),
                        "status",
                        status,
                        "items",
                        list.stream().map(s -> s.toSummaryMap()).toList()));
    }

    @GetMapping("/signals/{id}")
    public ApiResponse<?> signalDetail(@PathVariable("id") long id) {
        return ApiResponse.ok(strategyAnalysisService.getSignal(id).toMap());
    }

    @GetMapping("/article/{articleId}/signals")
    public ApiResponse<?> signalsByArticle(@PathVariable("articleId") long articleId) {
        var list = strategyAnalysisService.listByArticle(articleId);
        return ApiResponse.ok(
                Map.of(
                        "articleId",
                        articleId,
                        "items",
                        list.stream().map(s -> s.toSummaryMap()).toList()));
    }
}
