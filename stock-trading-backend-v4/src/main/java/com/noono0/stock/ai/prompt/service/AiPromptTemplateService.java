package com.noono0.stock.ai.prompt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.ai.prompt.domain.AiPromptTemplate;
import com.noono0.stock.ai.prompt.domain.AiPromptTestLog;
import com.noono0.stock.ai.prompt.repository.AiPromptTemplateRepository;
import com.noono0.stock.ai.prompt.repository.AiPromptTestLogRepository;
import com.noono0.stock.llm.client.OpenAiChatClient;
import com.noono0.stock.llm.util.LlmMessageTemplateRenderer;
import com.noono0.stock.llm.util.LlmNewsAnalysisParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPromptTemplateService {
    public static final String DEFAULT_NEWS_CODE = "NEWS_ANALYSIS";

    private final AiPromptTemplateRepository templateRepository;
    private final AiPromptTestLogRepository testLogRepository;
    private final OpenAiChatClient openAiChatClient;
    private final ObjectMapper objectMapper;

    public List<AiPromptTemplate> listAll() {
        return templateRepository.findAllByOrderByPromptCodeAscVersionDesc();
    }

    public List<AiPromptTemplate> listVersions(String promptCode) {
        return templateRepository.findByPromptCodeOrderByVersionDesc(promptCode);
    }

    public Optional<AiPromptTemplate> findById(long id) {
        return templateRepository.findById(id);
    }

    public Optional<AiPromptTemplate> getActive(String promptCode) {
        return templateRepository.findByPromptCodeAndIsActiveTrue(promptCode);
    }

    @Transactional
    public AiPromptTemplate create(
            String promptCode,
            String promptName,
            String promptType,
            String modelName,
            String systemPrompt,
            String userPrompt,
            String responseFormat,
            BigDecimal temperature,
            Integer maxTokens,
            String description) {
        if (!StringUtils.hasText(promptCode)) {
            throw new IllegalArgumentException("promptCode 필수");
        }
        String code = promptCode.trim().toUpperCase();
        int version = templateRepository.findTopByPromptCodeOrderByVersionDesc(code).map(t -> t.getVersion() + 1).orElse(1);
        AiPromptTemplate t = new AiPromptTemplate();
        t.setPromptCode(code);
        t.setPromptName(promptName);
        t.setPromptType(promptType != null ? promptType : "NEWS");
        t.setModelName(modelName != null ? modelName : "gpt-4o-mini");
        t.setSystemPrompt(systemPrompt);
        t.setUserPrompt(userPrompt);
        t.setResponseFormat(responseFormat);
        t.setTemperature(temperature != null ? temperature : new BigDecimal("0.20"));
        t.setMaxTokens(maxTokens != null ? maxTokens : 2000);
        t.setDescription(description);
        t.setVersion(version);
        t.setIsActive(false);
        AiPromptTemplate saved = templateRepository.save(t);
        log.info("【AI-PROMPT】 생성 code={} version={} id={}", code, version, saved.getId());
        return saved;
    }

    /** 새 버전 행 생성 (기존 행 내용 복사·수정) */
    @Transactional
    public AiPromptTemplate createNewVersion(
            long baseId,
            String promptName,
            String systemPrompt,
            String userPrompt,
            String responseFormat,
            BigDecimal temperature,
            Integer maxTokens,
            String description) {
        AiPromptTemplate base =
                templateRepository.findById(baseId).orElseThrow(() -> new IllegalArgumentException("템플릿 없음"));
        return create(
                base.getPromptCode(),
                promptName != null ? promptName : base.getPromptName(),
                base.getPromptType(),
                base.getModelName(),
                systemPrompt != null ? systemPrompt : base.getSystemPrompt(),
                userPrompt != null ? userPrompt : base.getUserPrompt(),
                responseFormat != null ? responseFormat : base.getResponseFormat(),
                temperature != null ? temperature : base.getTemperature(),
                maxTokens != null ? maxTokens : base.getMaxTokens(),
                description != null ? description : base.getDescription());
    }

    @Transactional
    public AiPromptTemplate setActive(long id) {
        AiPromptTemplate pick =
                templateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("템플릿 없음"));
        var versions = templateRepository.findByPromptCodeOrderByVersionDesc(pick.getPromptCode());
        for (AiPromptTemplate t : versions) {
            t.setIsActive(t.getId() != null && t.getId().equals(pick.getId()));
        }
        templateRepository.saveAll(versions);
        log.warn("【AI-PROMPT】 ★ 활성화 ★ code={} version={}", pick.getPromptCode(), pick.getVersion());
        return pick;
    }

    @Transactional
    public void setActiveFlag(long id, boolean active) {
        AiPromptTemplate t =
                templateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("템플릿 없음"));
        if (active) {
            setActive(id);
        } else {
            t.setIsActive(false);
            templateRepository.save(t);
        }
    }

    public String renderUser(AiPromptTemplate t, Map<String, String> vars) {
        return LlmMessageTemplateRenderer.render(t.getUserPrompt(), vars);
    }

    public Map<String, String> newsVars(String stockCode, String stockName, String title, String summary) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("stockCode", stockCode == null ? "" : stockCode);
        m.put("stockName", stockName == null ? "" : stockName);
        m.put("title", title == null ? "" : title);
        m.put("summary", summary == null ? "" : summary);
        return m;
    }

    public record TestResult(
            long testLogId,
            long templateId,
            String promptCode,
            int version,
            String rawResponse,
            int impactScore,
            boolean success,
            String errorMessage) {}

    @Transactional
    public TestResult runTest(
            long templateId, String stockCode, String stockName, String title, String summary) {
        AiPromptTemplate t =
                templateRepository.findById(templateId).orElseThrow(() -> new IllegalArgumentException("템플릿 없음"));
        Map<String, String> input = newsVars(stockCode, stockName, title, summary);
        AiPromptTestLog logRow = new AiPromptTestLog();
        logRow.setTemplateId(t.getId());
        logRow.setPromptCode(t.getPromptCode());
        try {
            logRow.setInputJson(objectMapper.writeValueAsString(input));
        } catch (Exception exception) {
            logRow.setInputJson("{}");
        }
        try {
            String user = renderUser(t, input);
            double temp = t.getTemperature() != null ? t.getTemperature().doubleValue() : 0.2;
            String raw =
                    openAiChatClient.chatCompletions(
                            t.getModelName(), t.getSystemPrompt(), user, temp);
            int score = LlmNewsAnalysisParser.parse(raw).impactScore() != null
                    ? LlmNewsAnalysisParser.parse(raw).impactScore()
                    : 50;
            logRow.setRawResponse(raw);
            logRow.setParsedScore(score);
            logRow.setSuccess(true);
            testLogRepository.save(logRow);
            return new TestResult(
                    logRow.getId(), t.getId(), t.getPromptCode(), t.getVersion(), raw, score, true, null);
        } catch (Exception exception) {
            logRow.setSuccess(false);
            logRow.setErrorMessage(exception.getMessage());
            testLogRepository.save(logRow);
            return new TestResult(
                    logRow.getId(), t.getId(), t.getPromptCode(), t.getVersion(), null, 0, false, exception.getMessage());
        }
    }

    public List<AiPromptTestLog> testHistory(long templateId) {
        return testLogRepository.findTop20ByTemplateIdOrderByCreatedAtDesc(templateId);
    }
}
