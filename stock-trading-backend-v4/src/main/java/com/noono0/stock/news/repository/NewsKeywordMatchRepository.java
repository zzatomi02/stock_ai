package com.noono0.stock.news.repository;

import com.noono0.stock.news.domain.NewsKeywordMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsKeywordMatchRepository extends JpaRepository<NewsKeywordMatch, Long> {
    List<NewsKeywordMatch> findByNewsArticleIdOrderByCreatedAtDesc(Long newsArticleId);
}
