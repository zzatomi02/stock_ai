package com.noono0.stock.ai.platform.service;

import com.noono0.stock.ai.platform.domain.AiAnalysisJob;
import com.noono0.stock.ai.platform.domain.AiModelConfig;
import com.noono0.stock.ai.platform.enums.AiAnalysisType;
import com.noono0.stock.ai.platform.enums.AiExecutionTiming;
import com.noono0.stock.ai.platform.enums.AiJobStatus;
import com.noono0.stock.ai.platform.enums.AiTargetType;
import com.noono0.stock.ai.platform.repository.AiAnalysisJobRepository;
import com.noono0.stock.ai.platform.repository.AiAnalysisPromptTemplateRepository;
import com.noono0.stock.ai.platform.repository.AiModelConfigRepository;
import com.noono0.stock.ai.platform.repository.AiProviderConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiAnalysisJobService {

    private final AiJobDedupService dedupService;
    private final AiAnalysisJobRepository jobRepository;
    private final AiProviderConfigRepository providerRepository;
    private final AiModelConfigRepository modelRepository;
    private final AiAnalysisPromptTemplateRepository templateRepository;

    @Transactional
    public List<AiAnalysisJob> enqueueCompanyAnalysis(
            String stockCode, String stockName, AiExecutionTiming timing) {
        return enqueueCompanyAnalysis(stockCode, stockName, timing, Integer.MAX_VALUE);
    }

    @Transactional
    public List<AiAnalysisJob> enqueueCompanyAnalysis(
            String stockCode, String stockName, AiExecutionTiming timing, int maxProviders) {
        var template =
                templateRepository
                        .findFirstByAnalysisTypeAndActiveTrueOrderByUpdatedAtDesc(
                                AiAnalysisType.COMPANY_ANALYSIS.name())
                        .orElseThrow(() -> new IllegalStateException("COMPANY_ANALYSIS 템플릿 없음"));

        List<AiAnalysisJob> jobs = new ArrayList<>();
        int count = 0;
        for (var provider : providerRepository.findByEnabledTrueOrderByProviderTypeAsc()) {
            if (count >= maxProviders) {
                break;
            }
            List<AiModelConfig> models =
                    modelRepository.findByProviderTypeAndAnalysisTypeAndEnabledTrue(
                            provider.getProviderType(), AiAnalysisType.COMPANY_ANALYSIS.name());
            if (models.isEmpty()) {
                continue;
            }
            AiModelConfig model = models.get(0);
            AiAnalysisJob job = new AiAnalysisJob();
            job.setAnalysisType(AiAnalysisType.COMPANY_ANALYSIS.name());
            job.setTargetType(AiTargetType.STOCK.name());
            job.setStockCode(stockCode);
            job.setStockName(stockName);
            job.setProviderType(provider.getProviderType());
            job.setModelName(model.getModelName());
            job.setPromptTemplateId(template.getId());
            job.setExecutionTiming(timing.name());
            job.setStatus(AiJobStatus.READY.name());
            job.setPriority(10);
            String inputHash = hash(stockCode + "|" + provider.getProviderType() + "|" + template.getPromptVersion());
            if (dedupService.shouldSkipDuplicate(inputHash, timing)) {
                continue;
            }
            job.setInputHash(inputHash);
            job.setScheduledAt(LocalDateTime.now());
            jobs.add(jobRepository.save(job));
            count++;
        }
        return jobs;
    }

    @Transactional
    public List<AiAnalysisJob> enqueueCompanyAnalysis(
            String stockCode,
            String stockName,
            AiExecutionTiming timing,
            List<String> providerTypes) {
        var template =
                templateRepository
                        .findFirstByAnalysisTypeAndActiveTrueOrderByUpdatedAtDesc(
                                AiAnalysisType.COMPANY_ANALYSIS.name())
                        .orElseThrow(() -> new IllegalStateException("COMPANY_ANALYSIS 템플릿 없음"));
        List<AiAnalysisJob> jobs = new ArrayList<>();
        for (String pt : providerTypes) {
            var provider = providerRepository.findByProviderType(pt).orElse(null);
            if (provider == null || !provider.isEnabled()) {
                continue;
            }
            var models =
                    modelRepository.findByProviderTypeAndAnalysisTypeAndEnabledTrue(
                            pt, AiAnalysisType.COMPANY_ANALYSIS.name());
            if (models.isEmpty()) {
                continue;
            }
            AiAnalysisJob job = newJob(stockCode, stockName, timing, template, provider, models.get(0));
            if (dedupService.shouldSkipDuplicate(job.getInputHash(), timing)) {
                continue;
            }
            jobs.add(jobRepository.save(job));
        }
        return jobs;
    }

    private static AiAnalysisJob newJob(
            String stockCode,
            String stockName,
            AiExecutionTiming timing,
            com.noono0.stock.ai.platform.domain.AiAnalysisPromptTemplate template,
            com.noono0.stock.ai.platform.domain.AiProviderConfig provider,
            com.noono0.stock.ai.platform.domain.AiModelConfig model) {
        AiAnalysisJob job = new AiAnalysisJob();
        job.setAnalysisType(AiAnalysisType.COMPANY_ANALYSIS.name());
        job.setTargetType(AiTargetType.STOCK.name());
        job.setStockCode(stockCode);
        job.setStockName(stockName);
        job.setProviderType(provider.getProviderType());
        job.setModelName(model.getModelName());
        job.setPromptTemplateId(template.getId());
        job.setExecutionTiming(timing.name());
        job.setStatus(AiJobStatus.READY.name());
        job.setPriority(10);
        job.setInputHash(hash(stockCode + "|" + provider.getProviderType() + "|" + template.getPromptVersion()));
        job.setScheduledAt(java.time.LocalDateTime.now());
        return job;
    }

    @Transactional(readOnly = true)
    public List<AiAnalysisJob> listReady(int limit) {
        return jobRepository
                .findTop20ByStatusAndScheduledAtBeforeOrderByPriorityDescScheduledAtAsc(
                        AiJobStatus.READY.name(), LocalDateTime.now())
                .stream()
                .limit(limit)
                .toList();
    }

    private static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8))).substring(0, 32);
        } catch (Exception e) {
            return input;
        }
    }
}
