package com.noono0.stock.news.repository;

import com.noono0.stock.news.domain.NewsSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsSourceJpaRepository extends JpaRepository<NewsSource, Long> {
    List<NewsSource> findByEnabledTrueOrderByIdAsc();
}
