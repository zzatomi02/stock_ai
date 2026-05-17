package com.noono0.stock.ai.platform.repository;

import com.noono0.stock.ai.platform.domain.AiCompanyAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AiCompanyAnalysisQueryRepository extends JpaRepository<AiCompanyAnalysis, Long> {

    @Query(
            """
            SELECT a FROM AiCompanyAnalysis a
            WHERE a.tradeDate = :tradeDate
              AND (:stockCode IS NULL OR a.stockCode = :stockCode)
              AND (:decision IS NULL OR a.decision = :decision)
              AND (:providerType IS NULL OR a.providerType = :providerType)
              AND (:minScore IS NULL OR a.overallScore >= :minScore)
            ORDER BY a.overallScore DESC, a.createdAt DESC
            """)
    List<AiCompanyAnalysis> findTodayFiltered(
            @Param("tradeDate") LocalDate tradeDate,
            @Param("stockCode") String stockCode,
            @Param("decision") String decision,
            @Param("providerType") String providerType,
            @Param("minScore") java.math.BigDecimal minScore);
}
