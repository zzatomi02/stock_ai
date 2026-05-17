package com.noono0.stock.tradingflow.repository;

import com.noono0.stock.tradingflow.domain.StrategyImprovementRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StrategyImprovementRecommendationRepository
        extends JpaRepository<StrategyImprovementRecommendation, Long> {
    List<StrategyImprovementRecommendation> findByTradeDateOrderByCreatedAtDesc(LocalDate tradeDate);
}
