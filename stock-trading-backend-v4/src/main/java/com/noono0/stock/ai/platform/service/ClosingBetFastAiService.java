package com.noono0.stock.ai.platform.service;

import com.noono0.stock.ai.platform.client.AiChatResponse;
import com.noono0.stock.ai.platform.client.AiProviderClient;
import com.noono0.stock.ai.platform.client.AiProviderClientRegistry;
import com.noono0.stock.ai.platform.config.AiPlatformProperties;
import com.noono0.stock.ai.platform.domain.AiAnalysisPromptTemplate;
import com.noono0.stock.ai.platform.domain.AiCompanyAnalysis;
import com.noono0.stock.ai.platform.enums.AiAnalysisType;
import com.noono0.stock.ai.platform.enums.AiExecutionTiming;
import com.noono0.stock.ai.platform.enums.AiProviderType;
import com.noono0.stock.ai.platform.repository.AiAnalysisPromptTemplateRepository;
import com.noono0.stock.ai.platform.repository.AiCompanyAnalysisRepository;
import com.noono0.stock.ai.platform.util.AiCompanyAnalysisParser;
import com.noono0.stock.ai.platform.util.AiPromptRenderer;
import com.noono0.stock.strategy.domain.enums.StrategyType;
import com.noono0.stock.strategy.service.TradingClockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * 종가베팅 구간(15:10~15:20)에 AI 캐시가 없을 때 짧은 동기 호출(기본 3초).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClosingBetFastAiService {

    private final AiPlatformProperties platformProperties;
    private final AiExecutionPolicyService executionPolicy;
    private final CachedAiAnalysisQueryService cachedAiQuery;
    private final AiProviderClientRegistry clientRegistry;
    private final AiAnalysisPromptTemplateRepository templateRepository;
    private final AiAnalysisContextBuilder contextBuilder;
    private final AiCompanyAnalysisParser companyParser;
    private final AiCompanyAnalysisRepository companyAnalysisRepository;
    private final AiValidityService validityService;
    private final TradingClockService tradingClock;

    private final ExecutorService fastAiExecutor =
            Executors.newCachedThreadPool(
                    r -> {
                        Thread t = new Thread(r, "closing-bet-fast-ai");
                        t.setDaemon(true);
                        return t;
                    });

    public boolean isActiveWindow() {
        return platformProperties.isClosingBetFastAiEnabled() && executionPolicy.isClosingBetWindow();
    }

    /**
     * CLOSING_BET 전략·캐시 없음일 때만 시도. 성공 시 DB에 저장하고 true.
     */
    public boolean tryRefreshCacheIfNeeded(String stockCode, String stockName, String strategyType) {
        if (!StrategyType.CLOSING_BET.name().equals(strategyType)) {
            return false;
        }
        if (!isActiveWindow()) {
            return false;
        }
        if (!StringUtils.hasText(stockCode)) {
            return false;
        }
        if (cachedAiQuery.getBestForTrading(stockCode).isPresent()) {
            return false;
        }
        executionPolicy.assertLiveApiAllowed(AiExecutionTiming.CLOSING_BET_FAST);
        return runFastAnalysis(stockCode, stockName);
    }

    private boolean runFastAnalysis(String stockCode, String stockName) {
        AiProviderClient client =
                clientRegistry
                        .get(AiProviderType.OPENAI.name())
                        .filter(AiProviderClient::isConfigured)
                        .orElse(null);
        if (client == null) {
            log.warn("[CLOSING-FAST-AI] OpenAI 미설정 stock={}", stockCode);
            return false;
        }
        Optional<AiAnalysisPromptTemplate> templateOpt =
                templateRepository.findFirstByAnalysisTypeAndActiveTrueOrderByUpdatedAtDesc(
                        AiAnalysisType.COMPANY_ANALYSIS.name());
        if (templateOpt.isEmpty()) {
            return false;
        }
        AiAnalysisPromptTemplate template = templateOpt.get();
        Map<String, String> vars = contextBuilder.buildCompanyContext(stockCode, stockName);
        String system = template.getSystemPrompt() != null ? template.getSystemPrompt() : "";
        String user = AiPromptRenderer.render(template.getUserPromptTemplate(), vars);
        String model = platformProperties.getDefaultOpenAiModel();
        int maxTokens = platformProperties.getClosingBetFastAiMaxTokens();
        long timeoutMs = platformProperties.getClosingBetFastAiTimeoutMs();

        Future<AiChatResponse> future =
                fastAiExecutor.submit(
                        () -> client.chat(model, system, user, 0.1, maxTokens));

        try {
            AiChatResponse chat = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (!chat.success() || !StringUtils.hasText(chat.content())) {
                log.warn(
                        "[CLOSING-FAST-AI] 실패/빈응답 stock={} err={} elapsed={}ms",
                        stockCode,
                        chat.error(),
                        chat.elapsedMs());
                return false;
            }
            persist(stockCode, stockName, model, chat.content());
            log.info(
                    "[CLOSING-FAST-AI] ★ 저장 완료 ★ stock={} elapsed={}ms (limit {}ms)",
                    stockCode,
                    chat.elapsedMs(),
                    timeoutMs);
            return true;
        } catch (TimeoutException exception) {
            future.cancel(true);
            log.warn("[CLOSING-FAST-AI] timeout {}ms stock={}", timeoutMs, stockCode);
            return false;
        } catch (Exception exception) {
            log.warn("[CLOSING-FAST-AI] 오류 stock={} — {}", stockCode, exception.getMessage());
            return false;
        }
    }

    private void persist(String stockCode, String stockName, String model, String rawJson) throws Exception {
        AiCompanyAnalysis entity =
                companyParser.toEntity(
                        stockCode,
                        stockName,
                        tradingClock.today(),
                        AiProviderType.OPENAI.name(),
                        model,
                        rawJson);
        var window =
                validityService.windowFor(
                        AiExecutionTiming.CLOSING_BET_FAST, AiAnalysisType.COMPANY_ANALYSIS);
        entity.setValidFrom(window.validFrom());
        entity.setValidUntil(window.validUntil());
        companyAnalysisRepository.save(entity);
    }
}
