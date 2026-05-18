package com.noono0.stock.news.repository;

import com.noono0.stock.news.domain.NewsSentimentKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsSentimentKeywordRepository extends JpaRepository<NewsSentimentKeyword, Long> {
    List<NewsSentimentKeyword> findByIsActiveTrueOrderByKeywordTypeAscKeywordAsc();

    List<NewsSentimentKeyword> findByKeywordTypeAndIsActiveTrue(String keywordType);
}
