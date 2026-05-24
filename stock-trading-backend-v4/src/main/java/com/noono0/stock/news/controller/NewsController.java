package com.noono0.stock.news.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.news.dto.KeywordRuleRequest;
import com.noono0.stock.news.dto.KeywordRuleUpdateRequest;
import com.noono0.stock.news.service.NewsIngestService;
import com.noono0.stock.news.service.NewsReadService;
import com.noono0.stock.news.service.NewsScoringService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {
    private final NewsScoringService newsScoringService;
    private final NewsIngestService newsIngestService;
    private final NewsReadService newsReadService;

    @GetMapping("/rules")
    public ApiResponse<?> rules() {
        var list = newsScoringService.getRules();
        log.info("【NEWS-KEYWORD】 규칙 목록 API — {}건", list.size());
        return ApiResponse.ok(list);
    }

    /** DB에 수집된 뉴스(네이버 검색·ingest 등) — stockCode(6자리) 기준 */
    @GetMapping("/articles")
    public ApiResponse<?> articles(
            @RequestParam(name = "stockCode") String stockCode, @RequestParam(name = "limit") int limit) {
        return ApiResponse.ok(newsReadService.listByStockCode(stockCode, limit));
    }

    @PostMapping("/rules")
    public ApiResponse<?> createRule(@Valid @RequestBody KeywordRuleRequest request) {
        return ApiResponse.ok(
                newsScoringService.createRule(
                        request.keyword(), request.score(), request.polarity(), request.description()),
                "키워드 규칙이 저장되었습니다.");
    }

    @PutMapping("/rules/{id}")
    public ApiResponse<?> updateRule(
            @PathVariable("id") long id, @Valid @RequestBody KeywordRuleUpdateRequest request) {
        return ApiResponse.ok(
                newsScoringService.updateRule(
                        id, request.keyword(), request.score(), request.polarity(), request.description(), request.active()),
                "키워드 규칙이 수정되었습니다.");
    }

    @DeleteMapping("/rules/{id}")
    public ApiResponse<?> deleteRule(@PathVariable("id") long id) {
        newsScoringService.deleteRule(id);
        return ApiResponse.ok(Map.of("deleted", true, "id", id), "삭제되었습니다.");
    }

    /** 키워드 규칙만으로 점수 시뮬(저장 없음) */
    @PostMapping("/score/preview")
    public ApiResponse<?> scorePreview(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(
                newsScoringService.scorePreview(
                        body.getOrDefault("stockCode", "000000"),
                        body.getOrDefault("title", ""),
                        body.getOrDefault("summary", "")));
    }

    @PostMapping("/score")
    public ApiResponse<?> score(@RequestBody Map<String, String> request) {
        return ApiResponse.ok(
                newsScoringService.scoreAndSave(
                        request.getOrDefault("stockCode", "005930"),
                        request.getOrDefault("title", "MOU 체결"),
                        request.getOrDefault("summary", "수주 기대감")));
    }

    /** 네이버 검색 API 실호출 → DB 저장 (중복 제거). query·display·stockCode 는 호출 측(프론트 등)에서 지정. */
    @PostMapping("/ingest")
    public ApiResponse<?> ingest(HttpServletRequest request) {
        String query = request.getParameter("query");
        String displayRaw = request.getParameter("display");
        String stockCode = request.getParameter("stockCode");
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query는 필수입니다.");
        }
        if (displayRaw == null || displayRaw.isBlank()) {
            throw new IllegalArgumentException("display는 필수입니다.");
        }
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode는 필수입니다.");
        }
        int display;
        try {
            display = Integer.parseInt(displayRaw.trim());
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("display는 정수여야 합니다.");
        }
        return ApiResponse.ok(Map.of("saved", newsIngestService.ingest(query, display, stockCode)));
    }
}
