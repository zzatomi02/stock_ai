package com.noono0.stock.collect.repository;

import com.noono0.stock.collect.domain.NewsSearchKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NewsSearchKeywordRepository extends JpaRepository<NewsSearchKeyword, Long> {

    List<NewsSearchKeyword> findByEnabledTrueOrderByPriorityAscIdAsc();

    List<NewsSearchKeyword> findAllByOrderByPriorityAscIdAsc();
}
