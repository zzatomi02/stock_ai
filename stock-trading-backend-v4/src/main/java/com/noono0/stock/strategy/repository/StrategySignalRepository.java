package com.noono0.stock.strategy.repository;

import com.noono0.stock.strategy.domain.StrategySignal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StrategySignalRepository extends JpaRepository<StrategySignal, Long> {

    boolean existsByStrategyCodeAndNewsArticleIdAndTradeDate(
            String strategyCode, Long newsArticleId, LocalDate tradeDate);

    List<StrategySignal> findByNewsArticleIdOrderByCreatedAtDesc(Long newsArticleId);

    List<StrategySignal> findByTradeDateAndStatusOrderByAdjustedFinalScoreDesc(
            LocalDate tradeDate, String status);

    Optional<StrategySignal> findTopByStrategyCodeAndNewsArticleIdAndTradeDate(
            String strategyCode, Long newsArticleId, LocalDate tradeDate);

    long countByTradeDateAndStatus(LocalDate tradeDate, String status);
}
