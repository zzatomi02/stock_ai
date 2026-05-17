package com.noono0.stock.llm.service;

import com.noono0.stock.ai.prompt.domain.AiPromptTemplate;
import com.noono0.stock.ai.prompt.service.AiPromptTemplateService;
import com.noono0.stock.llm.client.OpenAiChatClient;
import com.noono0.stock.llm.config.LlmOpenAiProperties;
import com.noono0.stock.llm.domain.LlmQuestionTemplate;
import com.noono0.stock.llm.dto.LlmNewsAnalysisDto;
import com.noono0.stock.llm.util.LlmMessageTemplateRenderer;
import com.noono0.stock.llm.util.LlmNewsAnalysisParser;
import com.noono0.stock.news.domain.NewsArticle;
import com.noono0.stock.news.repository.NewsArticleJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 뉴스 DB 저장 이후(옵션) OpenAI로 ai_score·LLM 원문을 채움.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmNewsIngestEnricher {
    private static final int MAX_RAW = 16000;
    private final LlmOpenAiProperties openAiProperties;
    private final LlmQuestionTemplateService templateService;
    private final AiPromptTemplateService aiPromptTemplateService;
    private final OpenAiChatClient openAiChatClient;
    private final NewsArticleJpaRepository newsArticleJpaRepository;
    private final ObjectMapper objectMapper;

    /**
     * MyBatis insert 직후 같은 요청에서 호출. id가 채워진 {@link NewsArticle}로 갱신 저장한다.
     */
    public void enrichIfEnabled(NewsArticle a) {
        if (a.getId() == null) {
            return;
        }
        if (!openAiProperties.isAnalyzeOnIngest() || !StringUtils.hasText(openAiProperties.getApiKey())) {
            return;
        }
        var aiTpl = aiPromptTemplateService.getActive(AiPromptTemplateService.DEFAULT_NEWS_CODE);
        String model;
        String system;
        String user;
        String templateLabel;
        var vars =
                aiPromptTemplateService.newsVars(
                        a.getStockCode(), guessStockName(a), a.getTitle(), a.getSummary());
        if (aiTpl.isPresent()) {
            AiPromptTemplate t = aiTpl.get();
            model = t.getModelName();
            system = t.getSystemPrompt();
            user = aiPromptTemplateService.renderUser(t, vars);
            templateLabel = t.getPromptCode() + " v" + t.getVersion();
        } else {
            LlmQuestionTemplate t = templateService.getDefault().orElse(null);
            if (t == null) {
                log.warn("【LLM-INGEST】 활성 AI 프롬프트/기본 템플릿 없음 — id={} enrich 생략", a.getId());
                return;
            }
            model = t.getOpenaiModel();
            system = t.getSystemPrompt();
            user = LlmMessageTemplateRenderer.render(t.getUserMessageTemplate(), vars);
            templateLabel = t.getName();
        }
        log.info("【LLM-INGEST】 enrich articleId={} template={} model={}", a.getId(), templateLabel, model);
        double temp =
                aiTpl.map(t -> t.getTemperature() != null ? t.getTemperature().doubleValue() : 0.2).orElse(0.2);
        String content = openAiChatClient.chatCompletions(model, system, user, temp);
        LlmNewsAnalysisDto parsed = LlmNewsAnalysisParser.parse(content);
        int score = parsed.impactScore() != null ? parsed.impactScore() : 50;
        a.setAiScore(score);
        a.setLlmStockName(parsed.stockName());
        a.setLlmSentiment(parsed.sentiment());
        a.setLlmEventType(parsed.eventType());
        a.setLlmActionHint(parsed.actionHint());
        a.setLlmConfidence(parsed.confidence());
        a.setLlmSummary(parsed.summary());
        try {
            a.setLlmReasonJson(objectMapper.writeValueAsString(parsed.reason()));
            a.setLlmRiskJson(objectMapper.writeValueAsString(parsed.risk()));
        } catch (Exception ignored) {
            a.setLlmReasonJson("[]");
            a.setLlmRiskJson("[]");
        }
        if (StringUtils.hasText(parsed.stockCode())) {
            a.setStockCode(parsed.stockCode());
        }
        a.setLlmModel(model);
        a.setLlmTemplateName(templateLabel);
        a.setLlmAnalyzedAt(LocalDateTime.now());
        a.setLlmRawResponse(truncate(content, MAX_RAW));
        newsArticleJpaRepository.save(a);
        log.warn(
                "【LLM-INGEST】 ★ enrich 완료 ★ id={} ai_score={} actionHint={} keywordScore={}",
                a.getId(),
                score,
                parsed.actionHint(),
                a.getKeywordScore());
    }

    private static String guessStockName(NewsArticle a) {
        return a.getStockCode() == null ? "" : a.getStockCode();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "\n...[truncated]";
    }
}
