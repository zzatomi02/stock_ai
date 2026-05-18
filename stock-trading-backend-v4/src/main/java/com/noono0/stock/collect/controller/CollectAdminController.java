package com.noono0.stock.collect.controller;

import com.noono0.stock.collect.domain.NewsSearchKeyword;
import com.noono0.stock.collect.service.KindRssDisclosureCollectorService;
import com.noono0.stock.collect.service.NaverNewsCollectorService;
import com.noono0.stock.collect.service.NewsSearchKeywordService;
import com.noono0.stock.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/collect/admin")
@RequiredArgsConstructor
public class CollectAdminController {
    private final NewsSearchKeywordService keywordService;
    private final NaverNewsCollectorService naverNewsCollectorService;
    private final KindRssDisclosureCollectorService kindRssDisclosureCollectorService;

    @PostMapping("/news-keywords/{id}/run")
    public ApiResponse<Map<String, Object>> runKeyword(@PathVariable long id) {
        NewsSearchKeyword keyword = keywordService.get(id);
        int saved = naverNewsCollectorService.collectByKeyword(keyword);
        return ApiResponse.ok(Map.of("keywordId", id, "saved", saved));
    }

    @PostMapping("/kind-rss/run")
    public ApiResponse<Map<String, Object>> runKindRss() {
        int saved = kindRssDisclosureCollectorService.collect();
        return ApiResponse.ok(Map.of("saved", saved));
    }
}
