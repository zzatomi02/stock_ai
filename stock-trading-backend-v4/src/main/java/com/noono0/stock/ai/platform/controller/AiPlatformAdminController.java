package com.noono0.stock.ai.platform.controller;

import com.noono0.stock.ai.platform.domain.AiModelConfig;
import com.noono0.stock.ai.platform.domain.AiProviderConfig;
import com.noono0.stock.ai.platform.enums.AiExecutionTiming;
import com.noono0.stock.ai.platform.repository.AiModelConfigRepository;
import com.noono0.stock.ai.platform.repository.AiProviderConfigRepository;
import com.noono0.stock.ai.platform.service.AiAnalysisExecutorService;
import com.noono0.stock.ai.platform.service.AiAnalysisJobService;
import com.noono0.stock.ai.platform.service.AiExecutionPolicyService;
import com.noono0.stock.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/platform")
@RequiredArgsConstructor
public class AiPlatformAdminController {

    private final AiProviderConfigRepository providerRepository;
    private final AiModelConfigRepository modelRepository;
    private final AiAnalysisJobService jobService;
    private final AiAnalysisExecutorService executorService;
    private final AiExecutionPolicyService executionPolicy;

    @GetMapping("/providers")
    public ApiResponse<List<AiProviderConfig>> providers() {
        return ApiResponse.ok(providerRepository.findAll());
    }

    @GetMapping("/models")
    public ApiResponse<List<AiModelConfig>> models() {
        return ApiResponse.ok(modelRepository.findAll());
    }

    @GetMapping("/execution-policy")
    public ApiResponse<?> policy() {
        var timing = executionPolicy.resolveCurrentTiming();
        return ApiResponse.ok(
                Map.of(
                        "currentTiming", timing.name(),
                        "liveApiAllowed", executionPolicy.isLiveApiCallAllowed(timing),
                        "message",
                        executionPolicy.isLiveApiCallAllowed(timing)
                                ? "배치 AI 호출 가능"
                                : "장중 — DB 캐시만 사용"));
    }

    @PostMapping("/jobs/company-analysis")
    public ApiResponse<?> enqueueCompany(
            @RequestParam(name = "stockCode") String stockCode,
            @RequestParam(name = "stockName", required = false) String stockName,
            @RequestParam(name = "timing", defaultValue = "MANUAL") String timing) {
        AiExecutionTiming t = AiExecutionTiming.valueOf(timing);
        executionPolicy.assertLiveApiAllowed(t);
        return ApiResponse.ok(
                jobService.enqueueCompanyAnalysis(
                        stockCode, stockName != null ? stockName : stockCode, t));
    }

    @PostMapping("/jobs/{jobId}/run")
    public ApiResponse<?> runJob(@PathVariable long jobId) {
        executorService.processJob(jobId);
        return ApiResponse.ok(Map.of("jobId", jobId, "status", "processed"));
    }
}
