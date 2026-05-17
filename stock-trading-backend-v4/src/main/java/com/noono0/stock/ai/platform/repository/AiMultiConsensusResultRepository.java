package com.noono0.stock.ai.platform.repository;

import com.noono0.stock.ai.platform.domain.AiMultiConsensusResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiMultiConsensusResultRepository extends JpaRepository<AiMultiConsensusResult, Long> {
    Optional<AiMultiConsensusResult> findTopByStockCodeAndTradeDateAndAnalysisTypeOrderByCreatedAtDesc(
            String stockCode, LocalDate tradeDate, String analysisType);
}
