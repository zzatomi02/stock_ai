package com.noono0.stock.collect.controller;

import com.noono0.stock.collect.domain.NewsSearchKeyword;
import com.noono0.stock.collect.dto.NewsSearchKeywordRequest;
import com.noono0.stock.collect.service.NewsSearchKeywordService;
import com.noono0.stock.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collect/news-keywords")
@RequiredArgsConstructor
public class NewsSearchKeywordController {
    private final NewsSearchKeywordService keywordService;

    @GetMapping
    public ApiResponse<List<NewsSearchKeyword>> list(
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String keywordGroup,
            @RequestParam(required = false) String searchType) {
        return ApiResponse.ok(keywordService.list(enabled, keywordGroup, searchType));
    }

    @GetMapping("/{id}")
    public ApiResponse<NewsSearchKeyword> detail(@PathVariable long id) {
        return ApiResponse.ok(keywordService.get(id));
    }

    @PostMapping
    public ApiResponse<NewsSearchKeyword> create(@Valid @RequestBody NewsSearchKeywordRequest body) {
        return ApiResponse.ok(keywordService.create(body));
    }

    @PutMapping("/{id}")
    public ApiResponse<NewsSearchKeyword> update(@PathVariable long id, @Valid @RequestBody NewsSearchKeywordRequest body) {
        return ApiResponse.ok(keywordService.update(id, body));
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<NewsSearchKeyword> setEnabled(@PathVariable long id, @RequestBody Map<String, Boolean> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        return ApiResponse.ok(keywordService.setEnabled(id, enabled));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Boolean>> delete(@PathVariable long id) {
        keywordService.delete(id);
        return ApiResponse.ok(Map.of("deleted", true));
    }
}
