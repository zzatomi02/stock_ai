package com.noono0.stock.ai.platform.repository;

import com.noono0.stock.ai.platform.domain.AiAnalysisPromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiAnalysisPromptTemplateRepository extends JpaRepository<AiAnalysisPromptTemplate, Long> {
    Optional<AiAnalysisPromptTemplate> findFirstByAnalysisTypeAndActiveTrueOrderByUpdatedAtDesc(
            String analysisType);
}
