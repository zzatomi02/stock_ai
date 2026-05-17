package com.noono0.stock.news.repository;

import com.noono0.stock.news.domain.NewsKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsKeywordRepository extends JpaRepository<NewsKeyword, Long> {
    List<NewsKeyword> findByIsActiveTrueOrderByKeywordTypeAscKeywordAsc();

    List<NewsKeyword> findByKeywordTypeAndIsActiveTrue(String keywordType);
}
