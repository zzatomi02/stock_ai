package com.noono0.stock.news.repository;

import com.noono0.stock.news.domain.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NewsArticleJpaRepository extends JpaRepository<NewsArticle, Long> {
    boolean existsByDedupHash(String dedupHash);

    List<NewsArticle> findByCollectedAtAfterOrderByCollectedAtDesc(LocalDateTime after);
}
