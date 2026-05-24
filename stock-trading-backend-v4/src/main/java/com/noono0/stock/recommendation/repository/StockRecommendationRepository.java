package com.noono0.stock.recommendation.repository;

import com.noono0.stock.recommendation.domain.StockRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockRecommendationRepository extends JpaRepository<StockRecommendation, Long> {

    Optional<StockRecommendation> findByTradeDateAndStockCode(LocalDate tradeDate, String stockCode);

    List<StockRecommendation> findByTradeDateOrderByBestAdjustedScoreDesc(LocalDate tradeDate);

    List<StockRecommendation> findByTradeDateAndStatusOrderByBestAdjustedScoreDesc(
            LocalDate tradeDate, String status);

    long countByTradeDate(LocalDate tradeDate);
}
