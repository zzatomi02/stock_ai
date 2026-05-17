package com.noono0.stock.ai.platform.repository;

import com.noono0.stock.ai.platform.domain.AiAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiAnalysisResultRepository extends JpaRepository<AiAnalysisResult, Long> {
    List<AiAnalysisResult> findByStockCodeAndAnalysisTypeOrderByCreatedAtDesc(
            String stockCode, String analysisType);
}
