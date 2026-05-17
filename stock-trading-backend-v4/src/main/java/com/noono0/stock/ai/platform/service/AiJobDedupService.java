package com.noono0.stock.ai.platform.service;

import com.noono0.stock.ai.platform.enums.AiJobStatus;
import com.noono0.stock.ai.platform.repository.AiAnalysisJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** input_hash 기반 중복 AI 호출 방지 */
@Service
@RequiredArgsConstructor
public class AiJobDedupService {

    private final AiAnalysisJobRepository jobRepository;
    private final AiValidityService validityService;

    public boolean shouldSkipDuplicate(String inputHash, com.noono0.stock.ai.platform.enums.AiExecutionTiming timing) {
        if (inputHash == null || inputHash.isBlank()) {
            return false;
        }
        var window = validityService.windowFor(timing, com.noono0.stock.ai.platform.enums.AiAnalysisType.COMPANY_ANALYSIS);
        return jobRepository.existsByInputHashAndStatusAndFinishedAtAfter(
                inputHash, AiJobStatus.SUCCESS.name(), window.validFrom());
    }
}
