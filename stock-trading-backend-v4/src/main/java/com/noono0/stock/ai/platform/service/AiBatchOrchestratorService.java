package com.noono0.stock.ai.platform.service;

import com.noono0.stock.ai.platform.config.AiPlatformProperties;
import com.noono0.stock.ai.platform.domain.AiAnalysisJob;
import com.noono0.stock.ai.platform.enums.AiBatchPhase;
import com.noono0.stock.ai.platform.enums.AiExecutionTiming;
import com.noono0.stock.ai.platform.enums.AiJobStatus;
import com.noono0.stock.ai.platform.repository.AiAnalysisJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiBatchOrchestratorService {

    private final AiPlatformProperties properties;
    private final AiExecutionPolicyService executionPolicy;
    private final AiValidityService validityService;
    private final AiAnalysisTargetCollector targetCollector;
    private final AiAnalysisJobService jobService;
    private final AiAnalysisJobRepository jobRepository;
    private final AiAsyncJobProcessor asyncJobProcessor;
    private final AiAnalysisExecutorService syncExecutor;

    @Transactional
    public int runPhase(AiBatchPhase phase) {
        if (!properties.isBatchEnabled() || !validityService.isInBatchWindow(phase)) {
            return 0;
        }
        AiExecutionTiming timing = mapTiming(phase);
        if (!executionPolicy.isLiveApiCallAllowed(timing)) {
            log.debug("[AI-BATCH] phase={} API 호출 불가", phase);
            return 0;
        }

        List<AiAnalysisJob> enqueued = new ArrayList<>();
        int stockLimit = Math.max(1, properties.getBatchMaxStocksPerTick());
        for (AiAnalysisTargetCollector.StockTarget t :
                targetCollector.collect(phase).stream().limit(stockLimit).toList()) {
            enqueued.addAll(jobService.enqueueCompanyAnalysis(t.stockCode(), t.stockName(), timing, 2));
        }

        int processed = 0;
        List<AiAnalysisJob> ready = jobRepository
                .findTop20ByStatusAndScheduledAtBeforeOrderByPriorityDescScheduledAtAsc(
                        AiJobStatus.READY.name(), java.time.LocalDateTime.now());

        for (AiAnalysisJob job : ready.stream().limit(properties.getBatchMaxJobsPerTick()).toList()) {
            if (phase == AiBatchPhase.INTRADAY_ASYNC) {
                asyncJobProcessor.processJobAsync(job.getId());
            } else {
                syncExecutor.processJob(job.getId());
            }
            processed++;
        }
        log.info("[AI-BATCH] phase={} enqueued={} processed={}", phase, enqueued.size(), processed);
        return processed;
    }

    private static AiExecutionTiming mapTiming(AiBatchPhase phase) {
        return switch (phase) {
            case PRE_MARKET -> AiExecutionTiming.PRE_MARKET;
            case INTRADAY_ASYNC -> AiExecutionTiming.INTRADAY_ASYNC;
            case CLOSING_CANDIDATE -> AiExecutionTiming.CLOSING_CANDIDATE;
            case POST_MARKET -> AiExecutionTiming.POST_MARKET;
        };
    }
}
