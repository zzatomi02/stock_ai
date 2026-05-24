package com.noono0.stock.collect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.noono0.stock.collect.domain.CollectJobLog;
import com.noono0.stock.collect.domain.NewsSearchKeyword;
import com.noono0.stock.collect.domain.StockNews;
import com.noono0.stock.collect.enums.CollectJobStatus;
import com.noono0.stock.collect.repository.NewsSearchKeywordRepository;
import com.noono0.stock.collect.repository.StockNewsRepository;
import com.noono0.stock.collect.util.NewsDuplicateHashUtil;
import com.noono0.stock.collect.util.NewsTextUtil;
import com.noono0.stock.integration.naver.NaverNewsSearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverNewsCollectorService {
    private static final String JOB_NAME = "naver-news-search";
    private static final String PROVIDER = "NAVER";
    private static final Pattern PRESS_FROM_TITLE = Pattern.compile("<b>([^<]+)</b>");

    private final NaverNewsSearchClient naverNewsSearchClient;
    private final StockNewsRepository stockNewsRepository;
    private final NewsSearchKeywordRepository keywordRepository;
    private final CollectLogService collectLogService;

    @Transactional
    public int collectByKeyword(NewsSearchKeyword keyword) {
        CollectJobLog job =
                collectLogService.startJob(
                        JOB_NAME, PROVIDER, "NEWS_KEYWORD", keyword.getKeyword());
        int requestCount = 1;
        int responseCount = 0;
        int saved = 0;
        int duplicate = 0;
        int skipped = 0;
        int errors = 0;

        try {
            int display = Math.min(100, Math.max(1, keyword.getDisplayCount()));
            String sort = StringUtils.hasText(keyword.getSortType()) ? keyword.getSortType() : "date";
            JsonNode root = naverNewsSearchClient.search(keyword.getKeyword(), display, sort);
            JsonNode items = root.path("items");
            if (items.isArray()) {
                responseCount = items.size();
                for (JsonNode item : items) {
                    try {
                        SaveOutcome outcome = persistItem(keyword, item);
                        switch (outcome) {
                            case SAVED -> saved++;
                            case DUPLICATE -> duplicate++;
                            case SKIPPED -> skipped++;
                        }
                    } catch (Exception exception) {
                        errors++;
                        collectLogService.logError(
                                JOB_NAME,
                                PROVIDER,
                                "NEWS_ITEM",
                                keyword.getKeyword(),
                                exception.getClass().getSimpleName(),
                                exception.getMessage(),
                                exception,
                                null,
                                null);
                    }
                }
            }
            keyword.setLastCollectedAt(LocalDateTime.now());
            keywordRepository.save(keyword);

            CollectJobStatus status =
                    errors > 0
                            ? (saved > 0 ? CollectJobStatus.PARTIAL_SUCCESS : CollectJobStatus.FAILED)
                            : CollectJobStatus.SUCCESS;
            collectLogService.finishJob(
                    job,
                    requestCount,
                    responseCount,
                    saved,
                    duplicate,
                    skipped,
                    errors,
                    status,
                    "keywordId=" + keyword.getId() + " group=" + keyword.getKeywordGroup());
            log.info(
                    "【NAVER-COLLECT】 keyword={} saved={} dup={} skip={} err={}",
                    keyword.getKeyword(),
                    saved,
                    duplicate,
                    skipped,
                    errors);
            return saved;
        } catch (Exception exception) {
            errors++;
            collectLogService.logError(
                    JOB_NAME,
                    PROVIDER,
                    "NEWS_KEYWORD",
                    keyword.getKeyword(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception,
                    null,
                    null);
            collectLogService.finishJob(
                    job, requestCount, responseCount, saved, duplicate, skipped, errors, CollectJobStatus.FAILED, exception.getMessage());
            throw exception;
        }
    }

    private enum SaveOutcome {
        SAVED,
        DUPLICATE,
        SKIPPED
    }

    private SaveOutcome persistItem(NewsSearchKeyword keyword, JsonNode item) {
        String rawTitle = item.path("title").asText("");
        String title = NewsTextUtil.stripHtml(rawTitle);
        if (!StringUtils.hasText(title)) {
            return SaveOutcome.SKIPPED;
        }
        String naverLink = item.path("link").asText("");
        String originalLink = item.path("originallink").asText("");
        if (!StringUtils.hasText(naverLink) && !StringUtils.hasText(originalLink)) {
            return SaveOutcome.SKIPPED;
        }
        String desc = NewsTextUtil.stripHtml(item.path("description").asText(""));
        LocalDateTime publishedAt = parsePubDate(item.path("pubDate").asText(""));
        String hash = NewsDuplicateHashUtil.hash(originalLink, naverLink, title, publishedAt);
        if (stockNewsRepository.existsByDuplicateHash(hash)) {
            return SaveOutcome.DUPLICATE;
        }

        StockNews news = new StockNews();
        news.setProvider(PROVIDER);
        news.setKeywordId(keyword.getId());
        news.setKeyword(keyword.getKeyword());
        news.setKeywordGroup(keyword.getKeywordGroup());
        news.setSearchType(keyword.getSearchType());
        news.setTitle(title.length() > 500 ? title.substring(0, 500) : title);
        news.setDescription(desc);
        news.setOriginalLink(StringUtils.hasText(originalLink) ? originalLink : null);
        news.setNaverLink(StringUtils.hasText(naverLink) ? naverLink : null);
        news.setPressName(extractPress(rawTitle));
        news.setPublishedAt(publishedAt);
        news.setCollectedAt(LocalDateTime.now());
        if ("THEME".equalsIgnoreCase(keyword.getKeywordGroup())) {
            news.setRelatedTheme(keyword.getKeyword());
        }
        news.setDuplicateHash(hash);
        stockNewsRepository.save(news);
        return SaveOutcome.SAVED;
    }

    private static String extractPress(String rawTitle) {
        Matcher m = PRESS_FROM_TITLE.matcher(rawTitle);
        if (m.find()) {
            return NewsTextUtil.stripHtml(m.group(1));
        }
        return null;
    }

    private static LocalDateTime parsePubDate(String pub) {
        if (!StringUtils.hasText(pub)) {
            return null;
        }
        try {
            return LocalDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(pub));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
