package com.noono0.stock.news.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.news.dto.NewsSourceUpsertRequest;
import com.noono0.stock.news.service.NewsSourceCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/news/sources")
@RequiredArgsConstructor
public class NewsSourceController {
    private final NewsSourceCollectService newsSourceCollectService;

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(newsSourceCollectService.listSources());
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody NewsSourceUpsertRequest req) {
        return ApiResponse.ok(newsSourceCollectService.createSource(req), "뉴스 소스가 추가되었습니다.");
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable("id") long id, @RequestBody NewsSourceUpsertRequest req) {
        return ApiResponse.ok(newsSourceCollectService.updateSource(id, req), "뉴스 소스가 수정되었습니다.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable("id") long id) {
        newsSourceCollectService.deleteSource(id);
        return ApiResponse.ok(Map.of("deleted", true, "id", id), "뉴스 소스가 삭제되었습니다.");
    }

    @PostMapping("/{id}/run")
    public ApiResponse<?> runOne(@PathVariable("id") long id) {
        int saved = newsSourceCollectService.collectSource(id);
        return ApiResponse.ok(Map.of("sourceId", id, "saved", saved), "수집 실행이 완료되었습니다.");
    }

    @PostMapping("/run-all")
    public ApiResponse<?> runAll() {
        int saved = newsSourceCollectService.collectEnabledSources();
        return ApiResponse.ok(Map.of("saved", saved), "활성 소스 전체 수집이 완료되었습니다.");
    }
}
