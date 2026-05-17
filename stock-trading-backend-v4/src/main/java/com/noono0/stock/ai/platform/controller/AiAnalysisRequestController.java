package com.noono0.stock.ai.platform.controller;

import com.noono0.stock.ai.platform.domain.AiAnalysisJob;
import com.noono0.stock.ai.platform.dto.AiAnalysisRequestDto;
import com.noono0.stock.ai.platform.enums.AiExecutionTiming;
import com.noono0.stock.ai.platform.service.AiAnalysisExecutorService;
import com.noono0.stock.ai.platform.service.AiAnalysisJobService;
import com.noono0.stock.ai.platform.service.AiExecutionPolicyService;
import com.noono0.stock.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/analysis")
@RequiredArgsConstructor
public class AiAnalysisRequestController {

    private final AiExecutionPolicyService executionPolicy;
    private final AiAnalysisJobService jobService;
    private final AiAnalysisExecutorService executorService;

    @PostMapping("/request")
    public ApiResponse<?> request(@Valid @RequestBody AiAnalysisRequestDto req) {
        AiExecutionTiming timing =
                req.executionTiming() != null
                        ? AiExecutionTiming.valueOf(req.executionTiming())
                        : executionPolicy.resolveCurrentTiming();
        executionPolicy.assertLiveApiAllowed(timing);

        List<AiAnalysisJob> jobs;
        if (req.providerTypes() != null && !req.providerTypes().isEmpty()) {
            jobs =
                    jobService.enqueueCompanyAnalysis(
                            req.stockCode(),
                            req.stockName() != null ? req.stockName() : req.stockCode(),
                            timing,
                            req.providerTypes());
        } else {
            jobs =
                    jobService.enqueueCompanyAnalysis(
                            req.stockCode(),
                            req.stockName() != null ? req.stockName() : req.stockCode(),
                            timing);
        }
        for (AiAnalysisJob job : jobs) {
            executorService.processJob(job.getId());
        }
        return ApiResponse.ok(Map.of("jobs", jobs, "timing", timing.name()));
    }
}
