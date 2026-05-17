package com.noono0.stock.llm.repository;

import com.noono0.stock.llm.domain.LlmQuestionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LlmQuestionTemplateRepository extends JpaRepository<LlmQuestionTemplate, Long> {
    Optional<LlmQuestionTemplate> findByDefaultTemplateIsTrue();

    List<LlmQuestionTemplate> findAllByOrderByIdAsc();
}
