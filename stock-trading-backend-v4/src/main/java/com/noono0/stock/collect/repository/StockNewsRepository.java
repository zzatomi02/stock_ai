package com.noono0.stock.collect.repository;

import com.noono0.stock.collect.domain.StockNews;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockNewsRepository extends JpaRepository<StockNews, Long> {
    boolean existsByDuplicateHash(String duplicateHash);
}
