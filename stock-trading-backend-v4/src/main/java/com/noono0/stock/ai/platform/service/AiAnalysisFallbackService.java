package com.noono0.stock.ai.platform.service;

import com.noono0.stock.ai.platform.domain.AiAnalysisJob;
import com.noono0.stock.ai.platform.domain.AiCompanyAnalysis;
import com.noono0.stock.ai.platform.repository.AiCompanyAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** API 실패 시 최근 유효 캐시를 fallback으로 연결 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisFallbackService {

    private final AiCompanyAnalysisRepository companyAnalysisRepository;

    public Optional<AiCompanyAnalysis> findFallback(AiAnalysisJob job) {
        if (job.getStockCode() == null) {
            return Optional.empty();
        }
        Optional<AiCompanyAnalysis> cached = companyAnalysisRepository.findBestValidNow(job.getStockCode());
        if (cached.isPresent()) {
            log.info("[AI-FALLBACK] job={} stock={} — 캐시 분석 재사용 id={}", job.getId(), job.getStockCode(), cached.get().getId());
        }
        return cached;
    }
}
