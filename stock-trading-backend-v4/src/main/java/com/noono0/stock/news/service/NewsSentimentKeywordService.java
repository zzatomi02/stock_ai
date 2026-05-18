package com.noono0.stock.news.service;

import com.noono0.stock.news.domain.NewsKeywordMatch;
import com.noono0.stock.news.domain.NewsSentimentKeyword;
import com.noono0.stock.news.repository.NewsKeywordMatchRepository;
import com.noono0.stock.news.repository.NewsSentimentKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsSentimentKeywordService {
    private final NewsSentimentKeywordRepository keywordRepository;
    private final NewsKeywordMatchRepository matchRepository;

    public record MatchResult(
            int keywordScore,
            int positiveSum,
            int negativeSum,
            int riskSum,
            List<String> positiveKeywords,
            List<String> negativeKeywords,
            List<String> riskKeywords) {}

    public List<NewsSentimentKeyword> listAll() {
        return keywordRepository.findAll();
    }

    public List<NewsSentimentKeyword> listActive() {
        return keywordRepository.findByIsActiveTrueOrderByKeywordTypeAscKeywordAsc();
    }

    @Transactional
    public NewsSentimentKeyword create(
            String keyword, String keywordType, String category, Integer weight, String description) {
        NewsSentimentKeyword k = new NewsSentimentKeyword();
        k.setKeyword(keyword.trim());
        k.setKeywordType(keywordType.toUpperCase());
        k.setCategory(category);
        k.setWeight(weight != null ? weight : 10);
        k.setDescription(description);
        k.setIsActive(true);
        return keywordRepository.save(k);
    }

    @Transactional
    public NewsSentimentKeyword update(
            long id,
            String keyword,
            String keywordType,
            String category,
            Integer weight,
            String description,
            Boolean active) {
        NewsSentimentKeyword k =
                keywordRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("키워드 없음"));
        if (keyword != null) k.setKeyword(keyword.trim());
        if (keywordType != null) k.setKeywordType(keywordType.toUpperCase());
        if (category != null) k.setCategory(category);
        if (weight != null) k.setWeight(weight);
        if (description != null) k.setDescription(description);
        if (active != null) k.setIsActive(active);
        return keywordRepository.save(k);
    }

    @Transactional
    public void delete(long id) {
        keywordRepository.deleteById(id);
    }

    @Transactional
    public MatchResult matchAndSave(Long articleId, String stockCode, String title, String summary) {
        String source = ((title != null ? title : "") + " " + (summary != null ? summary : "")).toLowerCase();
        List<NewsSentimentKeyword> rules = keywordRepository.findByIsActiveTrueOrderByKeywordTypeAscKeywordAsc();
        int positive = 0;
        int negative = 0;
        int risk = 0;
        List<String> posKw = new ArrayList<>();
        List<String> negKw = new ArrayList<>();
        List<String> riskKw = new ArrayList<>();

        for (NewsSentimentKeyword rule : rules) {
            if (!source.contains(rule.getKeyword().toLowerCase())) {
                continue;
            }
            int w = rule.getWeight() != null ? rule.getWeight() : 10;
            switch (rule.getKeywordType()) {
                case "POSITIVE" -> {
                    positive += w;
                    posKw.add(rule.getKeyword());
                }
                case "NEGATIVE" -> {
                    negative += w;
                    negKw.add(rule.getKeyword());
                }
                case "RISK" -> {
                    risk += w;
                    riskKw.add(rule.getKeyword());
                }
                default -> {}
            }
            if (articleId != null) {
                NewsKeywordMatch m = new NewsKeywordMatch();
                m.setNewsArticleId(articleId);
                m.setKeywordId(rule.getId());
                m.setStockCode(stockCode);
                m.setKeyword(rule.getKeyword());
                m.setKeywordType(rule.getKeywordType());
                m.setWeight(w);
                m.setMatchedText(extractSnippet(source, rule.getKeyword()));
                matchRepository.save(m);
            }
        }
        int score = Math.max(0, Math.min(100, 50 + positive - negative - risk / 2));
        return new MatchResult(score, positive, negative, risk, posKw, negKw, riskKw);
    }

    public MatchResult matchPreview(String title, String summary) {
        return matchAndSave(null, null, title, summary);
    }

    public List<NewsKeywordMatch> matchHistory(long articleId) {
        return matchRepository.findByNewsArticleIdOrderByCreatedAtDesc(articleId);
    }

    private static String extractSnippet(String source, String keyword) {
        int idx = source.indexOf(keyword.toLowerCase());
        if (idx < 0) return keyword;
        int start = Math.max(0, idx - 20);
        int end = Math.min(source.length(), idx + keyword.length() + 20);
        return source.substring(start, end);
    }
}
