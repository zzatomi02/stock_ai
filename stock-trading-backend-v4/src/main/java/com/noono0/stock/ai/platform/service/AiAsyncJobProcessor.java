package com.noono0.stock.ai.platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** 장중 비동기 AI 작업 — 전략 스레드를 블로킹하지 않음 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAsyncJobProcessor {

    private final AiAnalysisExecutorService executorService;

    @Async("aiAnalysisExecutor")
    public void processJobAsync(long jobId) {
        try {
            executorService.processJob(jobId);
        } catch (Exception e) {
            log.warn("[AI-ASYNC] job {} 실패: {}", jobId, e.getMessage());
        }
    }
}
