package com.noono0.stock.ai.prompt.repository;

import com.noono0.stock.ai.prompt.domain.AiPromptTestLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiPromptTestLogRepository extends JpaRepository<AiPromptTestLog, Long> {
    List<AiPromptTestLog> findTop20ByTemplateIdOrderByCreatedAtDesc(Long templateId);
}
