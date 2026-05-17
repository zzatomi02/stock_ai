package com.noono0.stock.signal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.execution.ExecutionPhase;
import com.noono0.stock.execution.OrderGateway;
import com.noono0.stock.execution.config.ExecutionPhaseProperties;
import com.noono0.stock.llm.dto.LlmNewsAnalysisDto;
import com.noono0.stock.news.domain.NewsArticle;
import com.noono0.stock.signal.domain.TradingSignal;
import com.noono0.stock.order.service.OrderReasonService;
import com.noono0.stock.signal.repository.TradingSignalRepository;
import com.noono0.stock.strategy.service.StrategyExecutionResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalEngine {
    public static final String SYSTEM_USER = "__system__";

    private final TradingSignalRepository signalRepository;
    private final ExecutionPhaseProperties phaseProperties;
    private final OrderGateway orderGateway;
    private final ObjectMapper objectMapper;
    private final SignalScoringService signalScoringService;
    private final OrderReasonService orderReasonService;
    private final StrategyExecutionResolver strategyExecutionResolver;

    @Transactional
    public Optional<TradingSignal> generateFromArticle(NewsArticle article, String userId) {
        if (article == null || article.getId() == null) {
            return Optional.empty();
        }
        String uid = StringUtils.hasText(userId) ? userId.trim() : SYSTEM_USER;
        LlmNewsAnalysisDto analysis = buildAnalysisFromArticle(article);
        String actionHint = article.getLlmActionHint() != null ? article.getLlmActionHint() : analysis.actionHint();
        if (!StringUtils.hasText(actionHint) || "HOLD".equalsIgnoreCase(actionHint)) {
            actionHint = inferActionHintFromScores(article);
        }

        if ("IGNORE".equalsIgnoreCase(actionHint)) {
            return Optional.of(saveRejected(article, uid, analysis, actionHint, "GPT actionHint=IGNORE"));
        }

        String side = resolveSide(actionHint, analysis, article);
        if (side == null) {
            return Optional.empty();
        }

        if (signalRepository.existsByNewsArticleIdAndSide(article.getId(), side)) {
            return Optional.empty();
        }

        var breakdown = signalScoringService.compute(article, side);

        if ("BUY".equals(side) && !strategyExecutionResolver.hasAnyBuyStrategyAllowed(LocalDateTime.now())) {
            return Optional.of(saveRejected(
                    article, uid, analysis, actionHint, "현재 시장·시간대 설정상 매수 전략이 모두 비활성"));
        }
        if ("BUY".equals(side) && !signalScoringService.passesBuyRules(article, breakdown)) {
            return Optional.of(saveRejected(
                    article, uid, analysis, actionHint, "매수 조건 미충족 (점수=" + breakdown.finalScore() + ", 등급=" + breakdown.grade() + ")"));
        }
        if ("SELL".equals(side) && !signalScoringService.passesSellRules(article, breakdown)) {
            return Optional.of(saveRejected(
                    article, uid, analysis, actionHint, "매도 조건 미충족 (점수=" + breakdown.finalScore() + ")"));
        }
        if ("D".equals(breakdown.grade())) {
            return Optional.of(saveRejected(article, uid, analysis, actionHint, "등급 D — 관찰만"));
        }

        TradingSignal s = new TradingSignal();
        s.setUserId(uid);
        s.setStockCode(
                StringUtils.hasText(article.getStockCode()) ? article.getStockCode() : analysis.stockCode());
        s.setStockName(
                StringUtils.hasText(article.getLlmStockName())
                        ? article.getLlmStockName()
                        : analysis.stockName());
        s.setSide(side);
        s.setStatus("CANDIDATE");
        s.setKeywordScore(breakdown.keywordComponent());
        s.setAiScore(breakdown.aiComponent());
        s.setFinalScore(breakdown.finalScore());
        s.setSignalGrade(breakdown.grade());
        s.setMarketMoodScore(breakdown.marketComponent());
        s.setNewsArticleId(article.getId());
        s.setLlmSentiment(article.getLlmSentiment() != null ? article.getLlmSentiment() : analysis.sentiment());
        s.setLlmEventType(article.getLlmEventType() != null ? article.getLlmEventType() : analysis.eventType());
        s.setLlmActionHint(actionHint);
        s.setLlmConfidence(article.getLlmConfidence() != null ? article.getLlmConfidence() : analysis.confidence());
        s.setLlmSummary(article.getLlmSummary() != null ? article.getLlmSummary() : analysis.summary());
        s.setReasonSnapshot(buildReasonSnapshot(article, analysis, breakdown));
        s.setExecutionPhase(orderGateway.currentPhase().name());
        s.setCreatedAt(LocalDateTime.now());
        s.setExpiresAt(LocalDateTime.now().plusDays(1));
        signalRepository.save(s);
        orderReasonService.saveForSignal(s, "SIGNAL_CANDIDATE");
        log.info(
                "【SIGNAL】 ★ 후보 생성 ★ id={} grade={} side={} stock={} finalScore={}",
                s.getId(),
                breakdown.grade(),
                side,
                s.getStockCode(),
                breakdown.finalScore());
        return Optional.of(s);
    }

    private LlmNewsAnalysisDto buildAnalysisFromArticle(NewsArticle a) {
        return new LlmNewsAnalysisDto(
                a.getStockCode(),
                a.getLlmStockName(),
                a.getLlmSentiment(),
                a.getLlmEventType(),
                a.getAiScore(),
                a.getLlmConfidence(),
                a.getLlmSummary(),
                null,
                null,
                a.getLlmActionHint());
    }

    private String resolveSide(String actionHint, LlmNewsAnalysisDto analysis, NewsArticle article) {
        if ("WATCH_BUY".equalsIgnoreCase(actionHint)) {
            return "BUY";
        }
        if ("WATCH_SELL".equalsIgnoreCase(actionHint)) {
            return "SELL";
        }
        if ("HOLD".equalsIgnoreCase(actionHint) || "IGNORE".equalsIgnoreCase(actionHint)) {
            return null;
        }
        int ai = article.getAiScore() != null ? article.getAiScore() : 50;
        if ("POSITIVE".equalsIgnoreCase(analysis.sentiment()) && ai >= phaseProperties.getBuyScoreThreshold()) {
            return "BUY";
        }
        if ("NEGATIVE".equalsIgnoreCase(analysis.sentiment()) && ai <= phaseProperties.getSellScoreThreshold()) {
            return "SELL";
        }
        return null;
    }

    private TradingSignal saveRejected(
            NewsArticle article, String userId, LlmNewsAnalysisDto analysis, String actionHint, String reason) {
        TradingSignal s = new TradingSignal();
        s.setUserId(userId);
        s.setStockCode(article.getStockCode());
        s.setStockName(article.getLlmStockName());
        s.setSide(guessSideFromHint(actionHint));
        s.setStatus("REJECTED");
        s.setKeywordScore(article.getKeywordScore());
        s.setAiScore(article.getAiScore());
        s.setFinalScore(article.getAiScore());
        s.setSignalGrade("D");
        s.setNewsArticleId(article.getId());
        s.setLlmActionHint(actionHint);
        s.setRejectedReason(reason);
        s.setExecutionPhase(orderGateway.currentPhase().name());
        s.setCreatedAt(LocalDateTime.now());
        signalRepository.save(s);
        orderReasonService.saveForSignal(s, "SIGNAL_REJECTED");
        return s;
    }

    private String inferActionHintFromScores(NewsArticle article) {
        int kw = article.getKeywordScore() != null ? article.getKeywordScore() : 50;
        int ai = article.getAiScore() != null ? article.getAiScore() : 50;
        if (kw >= phaseProperties.getBuyScoreThreshold() || ai >= phaseProperties.getBuyScoreThreshold()) {
            return "WATCH_BUY";
        }
        if (kw <= phaseProperties.getSellScoreThreshold() || ai <= phaseProperties.getSellScoreThreshold()) {
            return "WATCH_SELL";
        }
        return "HOLD";
    }

    private static String guessSideFromHint(String hint) {
        if ("WATCH_SELL".equalsIgnoreCase(hint)) {
            return "SELL";
        }
        return "BUY";
    }

    private String buildReasonSnapshot(
            NewsArticle article, LlmNewsAnalysisDto analysis, SignalScoringService.ScoreBreakdown breakdown) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("articleId", article.getId());
        m.put("title", article.getTitle());
        m.put("keywordScore", breakdown.keywordComponent());
        m.put("aiScore", breakdown.aiComponent());
        m.put("marketMoodScore", breakdown.marketComponent());
        m.put("finalScore", breakdown.finalScore());
        m.put("grade", breakdown.grade());
        m.put("sentiment", analysis.sentiment());
        m.put("eventType", analysis.eventType());
        m.put("actionHint", analysis.actionHint());
        m.put("summary", analysis.summary());
        try {
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }
}
