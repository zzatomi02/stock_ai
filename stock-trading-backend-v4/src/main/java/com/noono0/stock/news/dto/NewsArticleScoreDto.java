package com.noono0.stock.news.dto;

import java.time.LocalDateTime;
import java.util.List;

public record NewsArticleScoreDto(Long id, String stockCode, String title, int keywordScore, int aiScore, int finalScore, List<String> matchedKeywords, LocalDateTime publishedAt) {}
