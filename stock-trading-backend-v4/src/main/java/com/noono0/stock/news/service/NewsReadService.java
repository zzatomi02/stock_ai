package com.noono0.stock.news.service;

import com.noono0.stock.news.domain.NewsArticle;
import com.noono0.stock.news.mapper.NewsArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsReadService {
    private final NewsArticleMapper newsArticleMapper;

    public List<NewsArticle> listByStockCode(String stockCode, int limit) {
        int n = Math.min(50, Math.max(1, limit));
        var rows = newsArticleMapper.findByStockCode(stockCode, n);
        log.info("【NEWS-DB】 종목 뉴스 목록 조회 ★ stockCode={} limit={} → {}건", stockCode, n, rows.size());
        return rows;
    }
}
