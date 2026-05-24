package com.noono0.stock.integration.naver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.integration.naver.config.NaverNewsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/** 네이버 뉴스 검색 API (실호출) */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaverNewsSearchClient {
    private final NaverNewsProperties props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public JsonNode search(String query, int display) {
        String sort = StringUtils.hasText(props.getSort()) ? props.getSort() : "date";
        return search(query, display, sort);
    }

    public JsonNode search(String query, int display, String sort) {
        if (!StringUtils.hasText(props.getClientId()) || !StringUtils.hasText(props.getClientSecret())) {
            throw new IllegalStateException("NAVER_CLIENT_ID / NAVER_CLIENT_SECRET 미설정");
        }
        if (!StringUtils.hasText(sort)) {
            sort = StringUtils.hasText(props.getSort()) ? props.getSort() : "date";
        }
        log.info("【NAVER-NEWS】 API 호출 준비 query={} display={} sort={}", query, display, sort);
        // build(true)는 이미 인코딩된 값 전용 — 한글 등은 build()로 인코딩되게 함
        String url =
                UriComponentsBuilder.fromHttpUrl("https://openapi.naver.com/v1/search/news.json")
                        .queryParam("query", query)
                        .queryParam("display", display)
                        .queryParam("sort", sort)
                        .build()
                        .encode()
                        .toUriString();
        try {
            String json =
                    restClient
                            .get()
                            .uri(url)
                            .headers(h -> {
                                h.set("X-Naver-Client-Id", props.getClientId());
                                h.set("X-Naver-Client-Secret", props.getClientSecret());
                            })
                            .retrieve()
                            .body(String.class);
            JsonNode tree = objectMapper.readTree(json);
            int n = tree.path("display").asInt(-1);
            int total = tree.path("total").asInt(-1);
            log.info("【NAVER-NEWS】 ★ 응답 수신 ★ display={} total={} (items 배열은 ingest에서 해석)", n, total);
            return tree;
        } catch (Exception exception) {
            log.error("【NAVER-NEWS】 요청 실패: {}", exception.getMessage());
            throw new IllegalStateException("네이버 뉴스 검색 실패: " + exception.getMessage(), exception);
        }
    }
}
