package com.noono0.stock.news.service;

import com.noono0.stock.llm.service.LlmNewsIngestEnricher;
import com.noono0.stock.news.domain.NewsArticle;
import com.noono0.stock.news.domain.NewsSource;
import com.noono0.stock.news.domain.StockSymbolMap;
import com.noono0.stock.news.dto.NewsArticleScoreDto;
import com.noono0.stock.news.dto.NewsSourceUpsertRequest;
import com.noono0.stock.news.mapper.StockSymbolMapMapper;
import com.noono0.stock.news.parser.NewsSourceParser;
import com.noono0.stock.news.parser.ParsedNewsArticle;
import com.noono0.stock.news.repository.NewsArticleJpaRepository;
import com.noono0.stock.news.repository.NewsSourceJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsSourceCollectService {
    private final NewsSourceJpaRepository newsSourceJpaRepository;
    private final NewsArticleJpaRepository newsArticleJpaRepository;
    private final StockSymbolMapMapper stockSymbolMapMapper;
    private final NewsScoringService newsScoringService;
    private final LlmNewsIngestEnricher llmNewsIngestEnricher;
    private final RestClient restClient;
    private final List<NewsSourceParser> parsers;

    @Transactional(readOnly = true)
    public List<NewsSource> listSources() {
        return newsSourceJpaRepository.findAll().stream()
                .sorted(Comparator.comparing(NewsSource::getId))
                .toList();
    }

    @Transactional
    public NewsSource createSource(NewsSourceUpsertRequest req) {
        NewsSource s = new NewsSource();
        apply(req, s);
        NewsSource saved = newsSourceJpaRepository.save(s);
        log.info("【NEWS-SOURCE】 소스 생성 id={} name={} parserKey={} enabled={}",
                saved.getId(), saved.getName(), saved.getParserKey(), saved.getEnabled());
        return saved;
    }

    @Transactional
    public NewsSource updateSource(long id, NewsSourceUpsertRequest req) {
        NewsSource s = newsSourceJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("news source not found: " + id));
        apply(req, s);
        NewsSource saved = newsSourceJpaRepository.save(s);
        log.info("【NEWS-SOURCE】 소스 수정 id={} name={} parserKey={} enabled={}",
                saved.getId(), saved.getName(), saved.getParserKey(), saved.getEnabled());
        return saved;
    }

    @Transactional
    public void deleteSource(long id) {
        newsSourceJpaRepository.deleteById(id);
        log.info("【NEWS-SOURCE】 소스 삭제 id={}", id);
    }

    @Transactional
    public int collectEnabledSources() {
        List<NewsSource> sources = newsSourceJpaRepository.findByEnabledTrueOrderByIdAsc();
        int total = 0;
        for (NewsSource s : sources) {
            total += collectOne(s);
        }
        log.info("【NEWS-COLLECT】 활성 소스 수집 완료 sources={} saved={}", sources.size(), total);
        return total;
    }

    @Transactional
    public int collectSource(long id) {
        NewsSource s = newsSourceJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("news source not found: " + id));
        return collectOne(s);
    }

    private int collectOne(NewsSource source) {
        NewsSourceParser parser = parserByKey(source.getParserKey());
        log.info("【NEWS-COLLECT】 소스 수집 시작 id={} name={} parserKey={} baseUrl={}",
                source.getId(), source.getName(), source.getParserKey(), source.getBaseUrl());
        String listHtml = fetchHtml(source.getBaseUrl());
        List<String> urls = parser.extractArticleUrls(source, listHtml);
        if (urls.isEmpty()) {
            log.warn("【NEWS-COLLECT】 기사 URL 추출 0건 id={} name={}", source.getId(), source.getName());
            return 0;
        }
        int max = Math.max(1, Optional.ofNullable(source.getMaxArticlesPerRun()).orElse(30));
        List<String> targets = urls.stream().limit(max).collect(Collectors.toList());
        List<StockSymbolMap> symbolMaps = stockSymbolMapMapper.findAll();
        int saved = 0;
        for (String articleUrl : targets) {
            try {
                String articleHtml = fetchHtml(articleUrl);
                ParsedNewsArticle parsed = parser.parseArticle(source, articleUrl, articleHtml);
                if (!StringUtils.hasText(parsed.title()) || !StringUtils.hasText(parsed.fullBody())) {
                    log.warn("【NEWS-COLLECT】 파싱 스킵(제목/본문 없음) sourceId={} url={}", source.getId(), articleUrl);
                    continue;
                }
                String dedup = DigestUtils.md5DigestAsHex((articleUrl + "\n" + parsed.title())
                        .getBytes(StandardCharsets.UTF_8));
                if (newsArticleJpaRepository.existsByDedupHash(dedup)) {
                    continue;
                }
                String stockCode = detectStockCode(symbolMaps, parsed.title(), parsed.fullBody());
                NewsArticleScoreDto score = newsScoringService.scorePreview(stockCode, parsed.title(), parsed.fullBody());
                NewsArticle row = new NewsArticle();
                row.setStockCode(stockCode);
                row.setTitle(parsed.title().length() > 500 ? parsed.title().substring(0, 500) : parsed.title());
                row.setSummary(buildSummary(parsed.fullBody()));
                row.setFullBody(parsed.fullBody());
                row.setSourceName(source.getName());
                row.setArticleUrl(articleUrl);
                row.setPublishedAt(parsed.publishedAt());
                row.setCollectedAt(LocalDateTime.now());
                row.setDedupHash(dedup);
                row.setKeywordScore(score.keywordScore());
                row.setAiScore(score.aiScore());
                newsArticleJpaRepository.save(row);
                try {
                    llmNewsIngestEnricher.enrichIfEnabled(row);
                } catch (Exception exception) {
                    log.warn("【LLM-INGEST】 enrich 실패(기사는 저장됨) id={} sourceId={} err={}",
                            row.getId(), source.getId(), exception.getMessage());
                }
                saved++;
            } catch (Exception exception) {
                log.warn("【NEWS-COLLECT】 기사 처리 실패 sourceId={} url={} err={}",
                        source.getId(), articleUrl, exception.getMessage());
            }
        }
        log.info("【NEWS-COLLECT】 소스 수집 완료 id={} name={} 추출={} 처리={} 저장={}",
                source.getId(), source.getName(), urls.size(), targets.size(), saved);
        return saved;
    }

    private String fetchHtml(String url) {
        return restClient.get()
                .uri(url)
                .headers(h -> h.set("User-Agent", "Mozilla/5.0 (StockAI/1.0; +https://localhost)"))
                .retrieve()
                .body(String.class);
    }

    private NewsSourceParser parserByKey(String key) {
        return parsers.stream()
                .filter(p -> p.parserKey().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 parserKey: " + key));
    }

    private static String detectStockCode(List<StockSymbolMap> maps, String title, String body) {
        String text = (title + " " + body).toLowerCase(Locale.ROOT);
        for (StockSymbolMap m : maps) {
            if (m == null || !StringUtils.hasText(m.getKisCode())) continue;
            String name = nz(m.getStockName()).toLowerCase(Locale.ROOT);
            String query = nz(m.getNaverQuery()).toLowerCase(Locale.ROOT);
            if (StringUtils.hasText(name) && text.contains(name)) return m.getKisCode().trim();
            if (StringUtils.hasText(query) && text.contains(query)) return m.getKisCode().trim();
        }
        return "000000";
    }

    private static String buildSummary(String fullBody) {
        String normalized = nz(fullBody).replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 280) return normalized;
        return normalized.substring(0, 280);
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }

    private static void apply(NewsSourceUpsertRequest req, NewsSource s) {
        if (!StringUtils.hasText(req.name())) throw new IllegalArgumentException("name은 필수입니다.");
        if (!StringUtils.hasText(req.baseUrl())) throw new IllegalArgumentException("baseUrl은 필수입니다.");
        if (!StringUtils.hasText(req.parserKey())) throw new IllegalArgumentException("parserKey는 필수입니다.");
        s.setName(req.name().trim());
        s.setBaseUrl(req.baseUrl().trim());
        s.setParserKey(req.parserKey().trim());
        s.setEnabled(req.enabled() == null ? true : req.enabled());
        s.setFetchIntervalSec(req.fetchIntervalSec() == null ? 300 : req.fetchIntervalSec());
        s.setMaxArticlesPerRun(req.maxArticlesPerRun() == null ? 30 : req.maxArticlesPerRun());
        s.setRequestHeadersJson(req.requestHeadersJson());
        s.setSelectorConfigJson(req.selectorConfigJson());
    }
}
