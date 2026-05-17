package com.noono0.stock.ai.platform.repository;

import com.noono0.stock.ai.platform.domain.AiModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiModelConfigRepository extends JpaRepository<AiModelConfig, Long> {
    List<AiModelConfig> findByAnalysisTypeAndEnabledTrueOrderByWeightDesc(String analysisType);

    List<AiModelConfig> findByProviderTypeAndAnalysisTypeAndEnabledTrue(
            String providerType, String analysisType);
}
