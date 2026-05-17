package com.noono0.stock.ai.platform.repository;

import com.noono0.stock.ai.platform.domain.AiAnalysisRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAnalysisRequestRepository extends JpaRepository<AiAnalysisRequest, Long> {}
