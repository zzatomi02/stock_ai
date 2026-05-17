package com.noono0.stock.news.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.news.domain.NewsKeyword;
import com.noono0.stock.news.service.NewsKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news/keywords")
@RequiredArgsConstructor
public class NewsKeywordController {
    private final NewsKeywordService keywordService;

    @GetMapping
    public ApiResponse<List<NewsKeyword>> list() {
        return ApiResponse.ok(keywordService.listActive());
    }

    @PostMapping
    public ApiResponse<NewsKeyword> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(
                keywordService.create(
                        String.valueOf(body.get("keyword")),
                        String.valueOf(body.get("keywordType")),
                        body.get("category") != null ? String.valueOf(body.get("category")) : null,
                        body.get("weight") != null ? Integer.parseInt(String.valueOf(body.get("weight"))) : 10,
                        body.get("description") != null ? String.valueOf(body.get("description")) : null));
    }

    @PutMapping("/{id}")
    public ApiResponse<NewsKeyword> update(@PathVariable("id") long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(
                keywordService.update(
                        id,
                        body.get("keyword") != null ? String.valueOf(body.get("keyword")) : null,
                        body.get("keywordType") != null ? String.valueOf(body.get("keywordType")) : null,
                        body.get("category") != null ? String.valueOf(body.get("category")) : null,
                        body.get("weight") != null ? Integer.parseInt(String.valueOf(body.get("weight"))) : null,
                        body.get("description") != null ? String.valueOf(body.get("description")) : null,
                        body.get("isActive") != null ? Boolean.parseBoolean(String.valueOf(body.get("isActive"))) : null));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable("id") long id) {
        keywordService.delete(id);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    @PostMapping("/match/preview")
    public ApiResponse<?> preview(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(
                keywordService.matchPreview(body.getOrDefault("title", ""), body.getOrDefault("summary", "")));
    }

    @GetMapping("/match/history/{articleId}")
    public ApiResponse<?> history(@PathVariable("articleId") long articleId) {
        return ApiResponse.ok(keywordService.matchHistory(articleId));
    }
}
