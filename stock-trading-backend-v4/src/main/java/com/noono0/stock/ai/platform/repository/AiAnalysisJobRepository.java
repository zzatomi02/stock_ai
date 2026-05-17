package com.noono0.stock.ai.platform.repository;

import com.noono0.stock.ai.platform.domain.AiAnalysisJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AiAnalysisJobRepository extends JpaRepository<AiAnalysisJob, Long> {
    List<AiAnalysisJob> findTop20ByStatusAndScheduledAtBeforeOrderByPriorityDescScheduledAtAsc(
            String status, LocalDateTime before);

    List<AiAnalysisJob> findByStockCodeAndAnalysisTypeOrderByCreatedAtDesc(
            String stockCode, String analysisType);

    boolean existsByInputHashAndStatusAndFinishedAtAfter(
            String inputHash, String status, java.time.LocalDateTime finishedAt);
}
