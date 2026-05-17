package com.noono0.stock.ai.platform.repository;

import com.noono0.stock.ai.platform.domain.AiCompanyAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AiCompanyAnalysisRepository extends JpaRepository<AiCompanyAnalysis, Long> {

    List<AiCompanyAnalysis> findByStockCodeAndTradeDateOrderByCreatedAtDesc(
            String stockCode, LocalDate tradeDate);

    @Query(
            """
            SELECT a FROM AiCompanyAnalysis a
            WHERE a.stockCode = :stockCode
              AND a.validFrom <= :now
              AND (a.validUntil IS NULL OR a.validUntil >= :now)
            ORDER BY a.overallScore DESC, a.createdAt DESC
            """)
    List<AiCompanyAnalysis> findValidForTrading(
            @Param("stockCode") String stockCode, @Param("now") LocalDateTime now);

    default Optional<AiCompanyAnalysis> findBestValidNow(String stockCode) {
        List<AiCompanyAnalysis> list = findValidForTrading(stockCode, LocalDateTime.now());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
