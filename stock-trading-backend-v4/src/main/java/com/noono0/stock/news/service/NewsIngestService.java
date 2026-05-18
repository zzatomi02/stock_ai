package com.noono0.stock.news.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.noono0.stock.integration.naver.NaverNewsSearchClient;
import com.noono0.stock.llm.service.LlmNewsIngestEnricher;
import com.noono0.stock.tradingflow.service.NewsArticleIngestPipeline;
import com.noono0.stock.news.domain.NewsArticle;
import com.noono0.stock.news.dto.NewsArticleScoreDto;
import com.noono0.stock.news.mapper.NewsArticleMapper;
import com.noono0.stock.news.repository.NewsSentimentKeywordRepository;
import com.noono0.stock.news.service.NewsSentimentKeywordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** 네이버 검색 → 기사 저장, URL+제목 해시로 중복 제거 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsIngestService {
    private final NaverNewsSearchClient naverNewsSearchClient;
    private final NewsArticleMapper newsArticleMapper;
    private final NewsScoringService newsScoringService;
    private final NewsSentimentKeywordService newsKeywordService;
    private final NewsSentimentKeywordRepository newsKeywordRepository;
    private final LlmNewsIngestEnricher llmNewsIngestEnricher;
    private final NewsArticleIngestPipeline ingestPipeline;

    @Transactional
    public int ingest(String query, int display, String defaultStockCode) {
        log.info("【NEWS-INGEST】 ═══ 네이버 뉴스 수집 시작 ═══ query={} display={} defaultStockCode={}", query, display, defaultStockCode);
        JsonNode root = naverNewsSearchClient.search(query, display);
        JsonNode items = root.path("items");
        int saved = 0;
        if (!items.isArray()) {
            log.warn("【NEWS-INGEST】 응답에 items 배열 없음 — 종료");
            return 0;
        }
        String sc = defaultStockCode != null ? defaultStockCode : "000000";
        for (JsonNode it : items) {
            String title = stripHtml(it.path("title").asText(""));
            String link = it.path("link").asText("");
            String desc = stripHtml(it.path("description").asText(""));
            String pub = it.path("pubDate").asText("");
            if (!StringUtils.hasText(link)) continue;
            String hash =
                    DigestUtils.md5DigestAsHex((link + "\n" + title).getBytes(StandardCharsets.UTF_8));
            if (newsArticleMapper.countByDedupHash(hash) > 0) continue;
            NewsArticleScoreDto preview;
            if (newsKeywordRepository.count() > 0) {
                var km = newsKeywordService.matchPreview(title, desc);
                preview =
                        new NewsArticleScoreDto(
                                null, sc, title, km.keywordScore(), 50, km.keywordScore(), km.positiveKeywords(), null);
            } else {
                preview = newsScoringService.scorePreview(sc, title, desc);
            }
            NewsArticle a = new NewsArticle();
            a.setStockCode(sc);
            a.setTitle(title.length() > 500 ? title.substring(0, 500) : title);
            a.setSummary(desc);
            a.setSourceName("naver-news");
            a.setArticleUrl(link);
            a.setPublishedAt(parsePub(pub));
            a.setCollectedAt(LocalDateTime.now());
            a.setDedupHash(hash);
            a.setKeywordScore(preview.keywordScore());
            a.setAiScore(preview.aiScore());
            newsArticleMapper.insert(a);
            if (newsKeywordRepository.count() > 0) {
                var km = newsKeywordService.matchAndSave(a.getId(), sc, title, desc);
                a.setKeywordScore(km.keywordScore());
            }
            try {
                llmNewsIngestEnricher.enrichIfEnabled(a);
            } catch (Exception e) {
                log.warn("【LLM-INGEST】 enrich 실패(기사는 저장됨) id={} — {}", a.getId(), e.getMessage());
            }
            ingestPipeline.onArticleSaved(a);
            saved++;
        }
        log.info("【NEWS-INGEST】 ★ 수집 완료 ★ 신규 저장 {}건 (전체 items {}개)", saved, items.size());
        return saved;
    }

    private static String stripHtml(String s) {
        if (s == null) return "";
        return s.replaceAll("<[^>]+>", "");
    }

    private static LocalDateTime parsePub(String pub) {
        if (!StringUtils.hasText(pub)) return null;
        try {
            return LocalDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(pub));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
