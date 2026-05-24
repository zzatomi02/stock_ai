package com.noono0.stock.ai.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.ai.platform.client.AiChatResponse;
import com.noono0.stock.ai.platform.client.AiProviderClient;
import com.noono0.stock.ai.platform.client.AiProviderClientRegistry;
import com.noono0.stock.ai.platform.domain.*;
import com.noono0.stock.ai.platform.enums.AiAnalysisType;
import com.noono0.stock.ai.platform.enums.AiExecutionTiming;
import com.noono0.stock.ai.platform.enums.AiJobStatus;
import com.noono0.stock.ai.platform.repository.*;
import com.noono0.stock.ai.platform.util.AiCompanyAnalysisParser;
import com.noono0.stock.ai.platform.util.AiPromptRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisExecutorService {

    private final AiExecutionPolicyService executionPolicy;
    private final AiAnalysisJobRepository jobRepository;
    private final AiAnalysisPromptTemplateRepository templateRepository;
    private final AiAnalysisRequestRepository requestRepository;
    private final AiAnalysisResultRepository resultRepository;
    private final AiCompanyAnalysisRepository companyAnalysisRepository;
    private final AiUsageLogRepository usageLogRepository;
    private final AiProviderClientRegistry clientRegistry;
    private final AiAnalysisContextBuilder contextBuilder;
    private final AiCompanyAnalysisParser companyParser;
    private final AiConsensusService consensusService;
    private final AiValidityService validityService;
    private final AiAnalysisFallbackService fallbackService;
    private final AiCostEstimatorService costEstimator;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processJob(long jobId) {
        AiAnalysisJob job =
                jobRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("job 없음"));
        AiExecutionTiming timing = AiExecutionTiming.valueOf(job.getExecutionTiming());
        executionPolicy.assertLiveApiAllowed(timing);

        job.setStatus(AiJobStatus.RUNNING.name());
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);

        try {
            execute(job);
            job.setStatus(AiJobStatus.SUCCESS.name());
        } catch (Exception exception) {
            job.setStatus(AiJobStatus.FAILED.name());
            job.setErrorMessage(exception.getMessage());
            log.error("[AI-JOB] 실패 id={} — {}", jobId, exception.getMessage());
            tryApplyFallback(job);
        }
        job.setFinishedAt(LocalDateTime.now());
        jobRepository.save(job);

        if (AiJobStatus.SUCCESS.name().equals(job.getStatus())
                && AiAnalysisType.COMPANY_ANALYSIS.name().equals(job.getAnalysisType())) {
            consensusService.buildCompanyConsensus(job.getStockCode(), java.time.LocalDate.now());
        }
    }

    private void execute(AiAnalysisJob job) throws Exception {
        AiAnalysisPromptTemplate template =
                templateRepository
                        .findById(job.getPromptTemplateId())
                        .orElseThrow(() -> new IllegalStateException("템플릿 없음"));

        AiProviderClient client =
                clientRegistry
                        .get(job.getProviderType())
                        .filter(AiProviderClient::isConfigured)
                        .orElseThrow(() -> new IllegalStateException("Provider 미설정: " + job.getProviderType()));

        Map<String, String> vars = contextBuilder.buildCompanyContext(job.getStockCode(), job.getStockName());
        String userPrompt = AiPromptRenderer.render(template.getUserPromptTemplate(), vars);
        String systemPrompt = template.getSystemPrompt() != null ? template.getSystemPrompt() : "";

        AiAnalysisRequest req = new AiAnalysisRequest();
        req.setJobId(job.getId());
        req.setProviderType(job.getProviderType());
        req.setModelName(job.getModelName());
        req.setAnalysisType(job.getAnalysisType());
        req.setPromptVersion(template.getPromptVersion());
        req.setInputHash(job.getInputHash());
        req.setRequestPayload(objectMapper.writeValueAsString(vars));
        req.setRenderedPrompt(systemPrompt + "\n---\n" + userPrompt);
        req = requestRepository.save(req);

        double temp = 0.2;
        AiChatResponse chat = client.chat(job.getModelName(), systemPrompt, userPrompt, temp, 3000);

        AiAnalysisResult res = new AiAnalysisResult();
        res.setJobId(job.getId());
        res.setRequestId(req.getId());
        res.setProviderType(job.getProviderType());
        res.setModelName(job.getModelName());
        res.setAnalysisType(job.getAnalysisType());
        res.setTargetType(job.getTargetType());
        res.setTargetId(job.getTargetId());
        res.setStockCode(job.getStockCode());
        res.setSuccess(chat.success());
        res.setRawResponse(chat.content());
        res.setErrorMessage(chat.error());
        res.setElapsedMs((int) chat.elapsedMs());

        if (chat.success() && chat.content() != null) {
            res.setParsedJson(companyParser.extractRawJson(chat.content()));
            persistCompanyAnalysis(job, chat.content());
        } else {
            tryApplyFallback(job);
        }
        resultRepository.save(res);
        logUsage(job, chat);
    }

    private void tryApplyFallback(AiAnalysisJob job) {
        fallbackService
                .findFallback(job)
                .ifPresent(
                        cached -> {
                            job.setErrorMessage(
                                    (job.getErrorMessage() != null ? job.getErrorMessage() + " | " : "")
                                            + "FALLBACK_CACHE id="
                                            + cached.getId());
                            jobRepository.save(job);
                        });
    }

    private void persistCompanyAnalysis(AiAnalysisJob job, String rawJson) throws Exception {
        if (!AiAnalysisType.COMPANY_ANALYSIS.name().equals(job.getAnalysisType())) {
            return;
        }
        AiCompanyAnalysis entity =
                companyParser.toEntity(
                        job.getStockCode(),
                        job.getStockName(),
                        java.time.LocalDate.now(),
                        job.getProviderType(),
                        job.getModelName(),
                        rawJson);
        AiExecutionTiming timing = AiExecutionTiming.valueOf(job.getExecutionTiming());
        var window = validityService.windowFor(timing, AiAnalysisType.COMPANY_ANALYSIS);
        entity.setValidFrom(window.validFrom());
        entity.setValidUntil(window.validUntil());
        companyAnalysisRepository.save(entity);
    }

    private void logUsage(AiAnalysisJob job, AiChatResponse chat) {
        AiUsageLog log = new AiUsageLog();
        log.setJobId(job.getId());
        log.setStockCode(job.getStockCode());
        log.setProviderType(job.getProviderType());
        log.setModelName(job.getModelName());
        log.setAnalysisType(job.getAnalysisType());
        log.setInputTokens(chat.inputTokens());
        log.setOutputTokens(chat.outputTokens());
        log.setEstimatedCost(
                costEstimator.estimate(job.getModelName(), chat.inputTokens(), chat.outputTokens()));
        log.setElapsedMs((int) chat.elapsedMs());
        log.setSuccess(chat.success());
        usageLogRepository.save(log);
    }
}
