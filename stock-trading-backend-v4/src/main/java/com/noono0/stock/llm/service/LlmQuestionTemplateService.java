package com.noono0.stock.llm.service;

import com.noono0.stock.llm.client.OpenAiChatClient;
import com.noono0.stock.llm.domain.LlmQuestionTemplate;
import com.noono0.stock.llm.repository.LlmQuestionTemplateRepository;
import com.noono0.stock.llm.util.LlmMessageTemplateRenderer;
import com.noono0.stock.llm.util.LlmScoreJsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmQuestionTemplateService {
    private final LlmQuestionTemplateRepository templateRepository;
    private final OpenAiChatClient openAiChatClient;

    public List<LlmQuestionTemplate> list() {
        return templateRepository.findAllByOrderByIdAsc();
    }

    public Optional<LlmQuestionTemplate> findById(long id) {
        return templateRepository.findById(id);
    }

    public Optional<LlmQuestionTemplate> getDefault() {
        return templateRepository.findByDefaultTemplateIsTrue();
    }

    @Transactional
    public LlmQuestionTemplate create(String name, String systemPrompt, String userMessageTemplate, String model, Double temperature) {
        ensureNameUnique(name, null);
        LlmQuestionTemplate t = new LlmQuestionTemplate();
        t.setName(name.trim());
        t.setSystemPrompt(systemPrompt);
        t.setUserMessageTemplate(userMessageTemplate);
        t.setOpenaiModel(model == null || model.isBlank() ? "gpt-4o-mini" : model.trim());
        t.setTemperature(temperature == null ? 0.2 : Math.max(0, Math.min(2, temperature)));
        t.setDefaultTemplate(false);
        LlmQuestionTemplate saved = templateRepository.save(t);
        log.info("【LLM-TEMPLATE】 생성 id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public LlmQuestionTemplate update(
            long id, String name, String systemPrompt, String userMessageTemplate, String model, Double temperature) {
        LlmQuestionTemplate t = templateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("템플릿 없음: " + id));
        if (name != null) {
            if (name.isBlank()) {
                throw new IllegalArgumentException("템플릿 이름이 비어 있습니다.");
            }
            if (!name.equals(t.getName())) {
                ensureNameUnique(name, id);
                t.setName(name.trim());
            }
        }
        if (systemPrompt != null) {
            t.setSystemPrompt(systemPrompt);
        }
        if (userMessageTemplate != null) {
            t.setUserMessageTemplate(userMessageTemplate);
        }
        if (model != null && !model.isBlank()) {
            t.setOpenaiModel(model.trim());
        }
        if (temperature != null) {
            t.setTemperature(Math.max(0, Math.min(2, temperature)));
        }
        LlmQuestionTemplate saved = templateRepository.save(t);
        log.info("【LLM-TEMPLATE】 수정 id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public void delete(long id) {
        templateRepository.deleteById(id);
        log.info("【LLM-TEMPLATE】 삭제 id={}", id);
    }

    @Transactional
    public void setAsDefault(long id) {
        LlmQuestionTemplate pick = templateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("템플릿 없음: " + id));
        List<LlmQuestionTemplate> all = templateRepository.findAll();
        for (LlmQuestionTemplate t : all) {
            t.setDefaultTemplate(t.getId() != null && t.getId().equals(pick.getId()));
        }
        templateRepository.saveAll(all);
        log.warn("【LLM-TEMPLATE】 ★ 기본 템플릿 지정 ★ id={} name={}", pick.getId(), pick.getName());
    }

    private void ensureNameUnique(String name, Long exceptId) {
        String n = name.trim();
        for (LlmQuestionTemplate t : templateRepository.findAllByOrderByIdAsc()) {
            if (exceptId != null && t.getId() != null && t.getId().equals(exceptId)) {
                continue;
            }
            if (n.equals(t.getName())) {
                throw new IllegalArgumentException("이미 사용 중인 템플릿 이름: " + n);
            }
        }
    }

    public String renderUserMessage(LlmQuestionTemplate t, Map<String, String> variables) {
        return LlmMessageTemplateRenderer.render(t.getUserMessageTemplate(), variables);
    }

    public Map<String, String> buildNewsVariables(
            String stockCode, String stockName, String title, String summary) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("stockCode", stockCode == null ? "" : stockCode);
        m.put("stockName", stockName == null ? "" : stockName);
        m.put("title", title == null ? "" : title);
        m.put("summary", summary == null ? "" : summary);
        return m;
    }

    public record PreviewResult(
            long templateId, String templateName, String model, String rawLlm, int parsedScore) {}

    public PreviewResult preview(long templateId, String stockCode, String stockName, String title, String summary) {
        LlmQuestionTemplate t =
                templateRepository.findById(templateId).orElseThrow(() -> new IllegalArgumentException("템플릿 없음: " + templateId));
        return runWithTemplate(t, stockCode, stockName, title, summary);
    }

    public PreviewResult previewWithDefault(
            String stockCode, String stockName, String title, String summary) {
        LlmQuestionTemplate t =
                getDefault()
                        .orElseThrow(
                                () -> new IllegalStateException("기본 템플릿이 지정되지 않았습니다. /api/llm/question-templates/{id}/set-default"));
        return runWithTemplate(t, stockCode, stockName, title, summary);
    }

    private PreviewResult runWithTemplate(
            LlmQuestionTemplate t, String stockCode, String stockName, String title, String summary) {
        Map<String, String> vars = buildNewsVariables(stockCode, stockName, title, summary);
        String user = LlmMessageTemplateRenderer.render(t.getUserMessageTemplate(), vars);
        log.info("【LLM-PREVIEW】 완성된 user 메시지 길이={} (미리보기 300자) … {}", user.length(), abbrev(user, 300));
        String content =
                openAiChatClient.chatCompletions(
                        t.getOpenaiModel(), t.getSystemPrompt(), user, t.getTemperature());
        int score = LlmScoreJsonParser.parseToScore0to100(content);
        log.info("【LLM-PREVIEW】 파싱 score={} (0~100)", score);
        return new PreviewResult(
                t.getId() == null ? 0L : t.getId(), t.getName(), t.getOpenaiModel(), content, score);
    }

    private static String abbrev(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…[+" + (s.length() - max) + "자]";
    }
}
