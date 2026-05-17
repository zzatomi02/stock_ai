package com.noono0.stock.news.service;

import com.noono0.stock.news.domain.NewsArticle;
import com.noono0.stock.news.domain.NewsKeywordRule;
import com.noono0.stock.news.dto.NewsArticleScoreDto;
import com.noono0.stock.news.mapper.NewsArticleMapper;
import com.noono0.stock.news.mapper.NewsKeywordRuleMapper;
import com.noono0.stock.news.repository.NewsArticleJpaRepository;
import com.noono0.stock.news.repository.NewsKeywordRepository;
import com.noono0.stock.tradingflow.service.NewsArticleIngestPipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsScoringService {
    private final NewsKeywordRuleMapper newsKeywordRuleMapper;
    private final NewsArticleMapper newsArticleMapper;
    private final NewsArticleIngestPipeline ingestPipeline;
    private final NewsKeywordService newsKeywordService;
    private final NewsKeywordRepository newsKeywordRepository;
    private final NewsArticleJpaRepository newsArticleJpaRepository;

    public List<NewsKeywordRule> getRules() {
        return newsKeywordRuleMapper.findAll();
    }

    @Transactional
    public NewsKeywordRule createRule(String keyword, Integer score, String polarity, String description) {
        NewsKeywordRule rule = new NewsKeywordRule();
        rule.setKeyword(keyword);
        rule.setScore(score);
        rule.setPolarity(polarity);
        rule.setDescription(description);
        rule.setActive(true);
        rule.setCreatedAt(LocalDateTime.now());
        newsKeywordRuleMapper.insert(rule);
        log.warn("【NEWS-KEYWORD】 ★ 규칙 등록 ★ id={} keyword={} score={} polarity={}", rule.getId(), rule.getKeyword(), rule.getScore(), rule.getPolarity());
        return rule;
    }

    @Transactional
    public NewsKeywordRule updateRule(
            long id, String keyword, Integer score, String polarity, String description, boolean active) {
        NewsKeywordRule r =
                newsKeywordRuleMapper
                        .findById(id);
        if (r == null) {
            throw new IllegalArgumentException("키워드 규칙이 없습니다: " + id);
        }
        r.setKeyword(keyword);
        r.setScore(score);
        r.setPolarity(polarity);
        r.setDescription(description);
        r.setActive(active);
        newsKeywordRuleMapper.update(r);
        log.warn("【NEWS-KEYWORD】 ★ 규칙 수정 ★ id={} keyword={} score={} active={}", id, keyword, score, active);
        return newsKeywordRuleMapper.findById(id);
    }

    @Transactional
    public void deleteRule(long id) {
        NewsKeywordRule r = newsKeywordRuleMapper.findById(id);
        if (r == null) {
            throw new IllegalArgumentException("키워드 규칙이 없습니다: " + id);
        }
        if (newsKeywordRuleMapper.deleteById(id) != 1) {
            throw new IllegalStateException("삭제에 실패했습니다: " + id);
        }
        log.warn("【NEWS-KEYWORD】 ★ 규칙 삭제 ★ id={} keyword={}", id, r.getKeyword());
    }

    /** 키워드·AI 점수만 계산 (저장 없음) — 수집 파이프라인에서 사용 */
    public NewsArticleScoreDto scorePreview(String stockCode, String title, String summary) {
        if (newsKeywordRepository.count() > 0) {
            var m = newsKeywordService.matchPreview(title, summary);
            return new NewsArticleScoreDto(
                    null,
                    stockCode,
                    title,
                    m.keywordScore(),
                    50,
                    m.keywordScore(),
                    m.positiveKeywords(),
                    LocalDateTime.now());
        }
        List<NewsKeywordRule> rules = newsKeywordRuleMapper.findByActiveTrue();
        List<String> matched = new ArrayList<>();
        int score = 50;
        String source = (title + " " + (summary != null ? summary : "")).toLowerCase();
        for (NewsKeywordRule rule : rules) {
            if (source.contains(rule.getKeyword().toLowerCase())) {
                matched.add(rule.getKeyword());
                score += rule.getScore();
            }
        }
        int keyword = Math.max(0, Math.min(100, score));
        int ai = 50;
        log.info("【NEWS-KEYWORD】 키워드 미리보기(저장 아님) — 매칭 {}건, keywordScore={}", matched.size(), keyword);
        return new NewsArticleScoreDto(
                null, stockCode, title, keyword, ai, Math.round((keyword + ai) / 2f), matched, LocalDateTime.now());
    }

    @Transactional
    public NewsArticleScoreDto scoreAndSave(String stockCode, String title, String summary) {
        if (newsKeywordRepository.count() > 0) {
            NewsArticle article = new NewsArticle();
            article.setStockCode(stockCode);
            article.setTitle(title);
            article.setSummary(summary);
            article.setPublishedAt(LocalDateTime.now());
            article.setCollectedAt(LocalDateTime.now());
            article.setSourceName("manual");
            article.setAiScore(50);
            newsArticleMapper.insert(article);
            var m = newsKeywordService.matchAndSave(article.getId(), stockCode, title, summary);
            article.setKeywordScore(m.keywordScore());
            newsArticleJpaRepository.save(article);
            ingestPipeline.onArticleSaved(article);
            return new NewsArticleScoreDto(
                    article.getId(),
                    stockCode,
                    title,
                    m.keywordScore(),
                    50,
                    m.keywordScore(),
                    m.positiveKeywords(),
                    article.getPublishedAt());
        }
        List<NewsKeywordRule> rules = newsKeywordRuleMapper.findByActiveTrue();
        List<String> matched = new ArrayList<>();
        int score = 50;
        String source = (title + " " + summary).toLowerCase();
        for (NewsKeywordRule rule : rules) {
            if (source.contains(rule.getKeyword().toLowerCase())) {
                matched.add(rule.getKeyword());
                score += rule.getScore();
            }
        }
        int finalScore = Math.max(0, Math.min(100, score));
        NewsArticle article = new NewsArticle();
        article.setStockCode(stockCode);
        article.setTitle(title);
        article.setSummary(summary);
        article.setPublishedAt(LocalDateTime.now());
        article.setCollectedAt(LocalDateTime.now());
        article.setSourceName("naver");
        article.setKeywordScore(finalScore);
        article.setAiScore(50);
        newsArticleMapper.insert(article);
        NewsArticle saved = article;
        ingestPipeline.onArticleSaved(saved);
        return new NewsArticleScoreDto(saved.getId(), stockCode, title, finalScore, 50, Math.round((finalScore + 50) / 2f), matched, saved.getPublishedAt());
    }
}
