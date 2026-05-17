package com.noono0.stock.ai.platform.repository;

import com.noono0.stock.ai.platform.domain.AiProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiProviderConfigRepository extends JpaRepository<AiProviderConfig, Long> {
    Optional<AiProviderConfig> findByProviderType(String providerType);

    List<AiProviderConfig> findByEnabledTrueOrderByProviderTypeAsc();
}
