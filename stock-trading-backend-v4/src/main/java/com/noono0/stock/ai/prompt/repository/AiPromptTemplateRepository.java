package com.noono0.stock.ai.prompt.repository;

import com.noono0.stock.ai.prompt.domain.AiPromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiPromptTemplateRepository extends JpaRepository<AiPromptTemplate, Long> {
    List<AiPromptTemplate> findByPromptCodeOrderByVersionDesc(String promptCode);

    List<AiPromptTemplate> findAllByOrderByPromptCodeAscVersionDesc();

    Optional<AiPromptTemplate> findByPromptCodeAndIsActiveTrue(String promptCode);

    Optional<AiPromptTemplate> findTopByPromptCodeOrderByVersionDesc(String promptCode);
}
