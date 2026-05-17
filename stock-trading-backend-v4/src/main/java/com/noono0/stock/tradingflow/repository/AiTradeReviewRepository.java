package com.noono0.stock.tradingflow.repository;

import com.noono0.stock.tradingflow.domain.AiTradeReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiTradeReviewRepository extends JpaRepository<AiTradeReview, Long> {
    Optional<AiTradeReview> findByTradeDateAndStrategySignalId(LocalDate tradeDate, Long strategySignalId);
}
