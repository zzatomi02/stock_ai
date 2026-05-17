package com.noono0.stock.strategy.service;

import com.noono0.stock.news.domain.NewsArticle;
import com.noono0.stock.news.repository.NewsArticleJpaRepository;
import com.noono0.stock.strategy.domain.StrategyMaster;
import com.noono0.stock.strategy.domain.StrategySignal;
import com.noono0.stock.strategy.domain.enums.MarketCondition;
import com.noono0.stock.strategy.domain.enums.MarketTimeWindow;
import com.noono0.stock.strategy.dto.MarketConditionAnalysis;
import com.noono0.stock.strategy.dto.ScoreAdjustmentResult;
import com.noono0.stock.strategy.dto.StrategyAnalysisResultDto;
import com.noono0.stock.strategy.dto.StrategyDecisionDto;
import com.noono0.stock.strategy.dto.StrategySignalResponseDto;
import com.noono0.stock.strategy.repository.StrategyMasterRepository;
import com.noono0.stock.strategy.repository.StrategySignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyAnalysisService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StrategyMasterRepository masterRepository;
    private final StrategyDecisionEngine decisionEngine;
    private final StrategySignalRepository strategySignalRepository;
    private final NewsArticleJpaRepository newsArticleRepository;
    private final TradingClockService tradingClock;
    private final MarketConditionAnalyzer marketConditionAnalyzer;
    private final MarketTimeWindowResolver marketTimeWindowResolver;
    private final StrategySignalEnrichmentService signalEnrichmentService;

    @Transactional
    public List<StrategyAnalysisResultDto> analyzeArticle(long articleId, LocalDateTime at) {
        NewsArticle article =
                newsArticleRepository
                        .findById(articleId)
                        .orElseThrow(() -> new IllegalArgumentException("기사를 찾을 수 없습니다: " + articleId));
        return analyzeArticle(article, at);
    }

    @Transactional
    public List<StrategyAnalysisResultDto> analyzeArticle(NewsArticle article, LocalDateTime at) {
        LocalDateTime kst = at.atZone(KST).toLocalDateTime();
        return tradingClock.runAt(kst, () -> analyzeArticleAtClock(article));
    }

    private List<StrategyAnalysisResultDto> analyzeArticleAtClock(NewsArticle article) {
        LocalDate tradeDate = tradingClock.today();
        var marketAnalysis = marketConditionAnalyzer.analyze();
        MarketTimeWindow window = marketTimeWindowResolver.resolve().window();

        List<StrategyAnalysisResultDto> results = new ArrayList<>();
        for (StrategyMaster master : masterRepository.findAllByOrderBySortOrderAscIdAsc()) {
            String strategyType = master.getStrategyType();
            if (strategySignalRepository.existsByStrategyCodeAndNewsArticleIdAndTradeDate(
                    strategyType, article.getId(), tradeDate)) {
                strategySignalRepository
                        .findTopByStrategyCodeAndNewsArticleIdAndTradeDate(
                                strategyType, article.getId(), tradeDate)
                        .ifPresent(
                                existing ->
                                        results.add(
                                                toResultFromExisting(master, existing, "이미 분석됨")));
                continue;
            }
            StrategyDecisionDto decision =
                    decisionEngine.decide(master, tradeDate, marketAnalysis, window, article);
            if (!decision.strategyExecutable()) {
                log.info(
                        "[STRATEGY-ANALYSIS] SKIP (비활성, DB 미저장) {} article={} — {}",
                        strategyType,
                        article.getId(),
                        decision.excludedReason());
                results.add(StrategyAnalysisResultDto.skipped(master, decision));
                continue;
            }
            results.add(analyzeOne(article, master, decision, tradeDate, marketAnalysis, window));
        }
        return results;
    }

    @Transactional
    public Map<String, Object> analyzeRecentArticles(int hours, LocalDateTime at) {
        LocalDateTime since = at.atZone(KST).toLocalDateTime().minusHours(Math.max(1, hours));
        List<NewsArticle> articles =
                newsArticleRepository.findByCollectedAtAfterOrderByCollectedAtDesc(since);
        int articleCount = 0;
        int signalCount = 0;
        List<Map<String, Object>> perArticle = new ArrayList<>();
        for (NewsArticle a : articles) {
            if (a.getId() == null) {
                continue;
            }
            List<StrategyAnalysisResultDto> one = analyzeArticle(a, at);
            articleCount++;
            signalCount += (int) one.stream().filter(r -> "CANDIDATE".equals(r.status())).count();
            perArticle.add(
                    Map.of(
                            "articleId",
                            a.getId(),
                            "title",
                            a.getTitle() != null ? a.getTitle() : "",
                            "results",
                            one.stream().map(StrategyAnalysisResultDto::toMap).toList()));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("articlesProcessed", articleCount);
        out.put("candidateSignals", signalCount);
        out.put("details", perArticle);
        return out;
    }

    public List<StrategySignalResponseDto> listByArticle(long articleId) {
        return strategySignalRepository.findByNewsArticleIdOrderByCreatedAtDesc(articleId).stream()
                .map(StrategySignalResponseDto::from)
                .toList();
    }

    public List<StrategySignalResponseDto> listSignals(LocalDate tradeDate, String status) {
        if (tradeDate == null) {
            tradeDate = LocalDate.now(KST);
        }
        String st = StringUtils.hasText(status) ? status : "CANDIDATE";
        return strategySignalRepository.findByTradeDateAndStatusOrderByAdjustedFinalScoreDesc(
                        tradeDate, st)
                .stream()
                .map(StrategySignalResponseDto::from)
                .toList();
    }

    public StrategySignalResponseDto getSignal(long id) {
        StrategySignal sig =
                strategySignalRepository
                        .findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("시그널을 찾을 수 없습니다: " + id));
        return StrategySignalResponseDto.from(sig);
    }

    private StrategyAnalysisResultDto analyzeOne(
            NewsArticle article,
            StrategyMaster master,
            StrategyDecisionDto decision,
            LocalDate tradeDate,
            MarketConditionAnalysis marketAnalysis,
            MarketTimeWindow window) {
        MarketCondition condition = marketAnalysis.condition();
        StringBuilder log = new StringBuilder();
        appendStep(log, 1, "tradeDate=" + tradeDate);
        appendStep(log, 2, "time=" + tradingClock.currentTime());
        appendStep(log, 3, "timeWindow=" + window.name() + " " + window.label());
        appendStep(
                log,
                4,
                "marketCondition="
                        + condition.name()
                        + " score="
                        + marketAnalysis.marketScore()
                        + " ("
                        + marketAnalysis.detectionSource()
                        + ")");
        appendStep(log, 5, "masterEnabled=" + decision.masterEnabled());
        appendStep(log, 6, "todayRuntimeAllows=" + decision.todayRuntimeAllows());
        appendStep(log, 7, "marketRuleAllows=" + decision.marketRuleAllows());
        appendStep(log, 8, "timeWindowAllows=" + decision.timeWindowAllows());

        double marketW = decision.marketWeightMultiplier();
        double timeW = decision.timeWeightMultiplier();
        double finalMultiplier = decision.finalWeightMultiplier();
        appendStep(log, 9, "marketWeight=" + marketW);
        appendStep(log, 10, "timeWeight=" + timeW);
        appendStep(log, 11, "finalWeightMultiplier=" + finalMultiplier);

        if (decision.componentScores() != null) {
            var c = decision.componentScores();
            appendStep(
                    log,
                    12,
                    "news="
                            + c.newsScore()
                            + " supply="
                            + c.supplyScore()
                            + " technical="
                            + c.technicalScore()
                            + " riskPenalty="
                            + c.riskPenalty());
        }

        String side =
                decision.signalType() != null && decision.signalType().name().equals("AVOID")
                        ? null
                        : inferSideFromDecision(decision);
        if (side == null) {
            appendStep(log, 13, "SKIP — side 미결정");
            return saveAndReturn(
                    master,
                    article,
                    decision,
                    tradeDate,
                    condition,
                    window,
                    null,
                    0,
                    0,
                    finalMultiplier,
                    marketW,
                    timeW,
                    "EXCLUDED",
                    "매매 방향(BUY/SELL)을 결정할 수 없음",
                    log.toString(),
                    false);
        }

        ScoreAdjustmentResult adj = decision.scoreAdjustment();
        int rawScore =
                adj != null
                        ? adj.compositeRawScore().intValue()
                        : (decision.componentScores() != null
                                ? decision.componentScores().compositeRawScore()
                                : 0);
        int adjusted = adj != null ? adj.adjustedFinalScore().intValue() : rawScore;
        appendStep(log, 14, "rawFinalScore=" + rawScore);
        appendStep(log, 15, "adjustedFinalScore=" + adjusted + " — " + (adj != null ? adj.formulaSummary() : ""));

        int minThreshold = decision.minScoreThreshold() != null ? decision.minScoreThreshold() : 62;
        boolean passedMin = decision.scoresPassThreshold();
        appendStep(log, 16, "minThreshold=" + minThreshold + " passed=" + passedMin);
        appendStep(log, 17, "rationaleSufficient=" + decision.rationaleSufficient());

        if (!passedMin) {
            appendStep(log, 18, "REJECTED — 최소점수 미달");
            return saveAndReturn(
                    master,
                    article,
                    decision,
                    tradeDate,
                    condition,
                    window,
                    side,
                    rawScore,
                    adjusted,
                    finalMultiplier,
                    marketW,
                    timeW,
                    "REJECTED",
                    "최소점수 미달 (필요>=" + minThreshold + ", actual=" + adjusted + ")",
                    log.toString(),
                    false);
        }

        if (!decision.rationaleSufficient()) {
            appendStep(log, 18, "EXCLUDED — 근거 부족");
            return saveAndReturn(
                    master,
                    article,
                    decision,
                    tradeDate,
                    condition,
                    window,
                    side,
                    rawScore,
                    adjusted,
                    finalMultiplier,
                    marketW,
                    timeW,
                    "EXCLUDED",
                    "매수/매도 근거 부족",
                    log.toString(),
                    false);
        }

        if (!decision.finalSignalAllowed()) {
            appendStep(log, 18, "EXCLUDED — 최종 신호 불허 (AI/Rule)");
            return saveAndReturn(
                    master,
                    article,
                    decision,
                    tradeDate,
                    condition,
                    window,
                    side,
                    rawScore,
                    adjusted,
                    finalMultiplier,
                    marketW,
                    timeW,
                    "EXCLUDED",
                    decision.decisionSummary(),
                    log.toString(),
                    false);
        }

        appendStep(log, 19, "enrich Rule+Risk+AI cache");
        return saveAndReturn(
                master,
                article,
                decision,
                tradeDate,
                condition,
                window,
                side,
                rawScore,
                adjusted,
                finalMultiplier,
                marketW,
                timeW,
                "CANDIDATE",
                decision.decisionSummary(),
                log.toString(),
                true);
    }

    private static String inferSideFromDecision(StrategyDecisionDto decision) {
        if (decision.signalType() == null) {
            return null;
        }
        return switch (decision.signalType()) {
            case BUY -> "BUY";
            case SELL -> "SELL";
            case WATCH -> "ANALYSIS";
            default -> null;
        };
    }

    private StrategyAnalysisResultDto saveAndReturn(
            StrategyMaster master,
            NewsArticle article,
            StrategyDecisionDto decision,
            LocalDate tradeDate,
            MarketCondition condition,
            MarketTimeWindow window,
            String side,
            int rawScore,
            int adjustedScore,
            double finalMultiplier,
            double marketW,
            double timeW,
            String status,
            String reason,
            String executionLog,
            boolean passedMin) {
        BigDecimal rawBd = scoreBd(rawScore);
        BigDecimal adjBd = scoreBd(adjustedScore);
        BigDecimal marketBd = weightBd(marketW);
        BigDecimal timeBd = weightBd(timeW);
        BigDecimal finalBd = weightBd(finalMultiplier);

        String selectedReason = "CANDIDATE".equals(status) ? reason : null;
        String excludedReason =
                "EXCLUDED".equals(status) || "REJECTED".equals(status) ? reason : null;

        StrategySignal sig = new StrategySignal();
        sig.setStrategyCode(master.getStrategyType());
        sig.setNewsArticleId(article.getId());
        sig.setStockCode(article.getStockCode());
        sig.setStockName(
                StringUtils.hasText(article.getLlmStockName())
                        ? article.getLlmStockName()
                        : article.getTitle());
        sig.setSide(side != null ? side : "ANALYSIS");
        sig.setTradeDate(tradeDate);
        sig.setMarketCondition(condition.name());
        sig.setMarketTimeWindow(window.name());
        sig.setMarketRegime(condition.name());
        sig.setTimeSlotCode(window.name());
        sig.setRawFinalScore(rawBd);
        sig.setAdjustedFinalScore(adjBd);
        sig.setMarketWeightMultiplier(marketBd);
        sig.setTimeWeightMultiplier(timeBd);
        sig.setFinalWeightMultiplier(finalBd);
        sig.setMarketWeight(marketW);
        sig.setTimeWeight(timeW);
        sig.setMinScoreThreshold(
                decision.minScoreThreshold() != null
                        ? decision.minScoreThreshold()
                        : (side != null ? 62 : 0));
        sig.setSignalGrade(gradeFromScore(adjustedScore));
        sig.setStatus(status);
        sig.setStrategySelectedReason(selectedReason);
        sig.setStrategyExcludedReason(excludedReason);
        sig.setReasonMessage(reason);
        sig.setExecutionLog(executionLog);
        sig.setCreatedAt(LocalDateTime.now(KST));

        if (decision.componentScores() != null && StringUtils.hasText(sig.getStockCode())) {
            var enrich =
                    signalEnrichmentService.enrich(sig, decision, decision.componentScores(), side);
            adjustedScore = enrich.finalBlendedScore();
            sig.setAdjustedFinalScore(scoreBd(adjustedScore));
            sig.setSignalGrade(gradeFromScore(adjustedScore));
            if (!enrich.riskPassed()) {
                status = "REJECTED";
                sig.setStatus(status);
                sig.setStrategyExcludedReason(enrich.riskBlockReason());
                sig.setReasonMessage(enrich.riskBlockReason());
            } else if (enrich.downgradedByAi()) {
                status = "EXCLUDED";
                sig.setStatus(status);
                sig.setStrategyExcludedReason("AI RISK/AVOID — WATCH_ONLY");
                sig.setReasonMessage("AI RISK/AVOID — WATCH_ONLY");
            }
            if (executionLog != null) {
                executionLog =
                        executionLog
                                + "[20] aiBlended="
                                + enrich.finalBlendedScore()
                                + " riskOk="
                                + enrich.riskPassed()
                                + "\n";
                sig.setExecutionLog(executionLog);
            }
        }

        strategySignalRepository.save(sig);

        log.info(
                "[STRATEGY-ANALYSIS] {} article={} status={} raw={} adj={} mult={}",
                master.getStrategyType(),
                article.getId(),
                status,
                rawBd,
                adjBd,
                finalBd);

        return toAnalysisResult(master, sig, passedMin, executionLog);
    }

    private static StrategyAnalysisResultDto toAnalysisResult(
            StrategyMaster master, StrategySignal sig, boolean passedMin, String executionLog) {
        return new StrategyAnalysisResultDto(
                master.getStrategyType(),
                master.getStrategyName(),
                "CANDIDATE".equals(sig.getStatus()) || "REJECTED".equals(sig.getStatus()),
                sig.getStatus(),
                sig.getSide(),
                sig.getMarketCondition(),
                sig.getMarketTimeWindow(),
                sig.getRawFinalScore(),
                sig.getAdjustedFinalScore(),
                sig.getMarketWeightMultiplier(),
                sig.getTimeWeightMultiplier(),
                sig.getFinalWeightMultiplier(),
                sig.getMinScoreThreshold(),
                sig.getSignalGrade(),
                passedMin,
                sig.getId(),
                sig.getStrategySelectedReason(),
                sig.getStrategyExcludedReason(),
                sig.getReasonMessage(),
                executionLog != null ? executionLog : sig.getExecutionLog());
    }

    private static StrategyAnalysisResultDto toResultFromExisting(
            StrategyMaster master, StrategySignal sig, String note) {
        return toAnalysisResult(master, sig, "CANDIDATE".equals(sig.getStatus()), note);
    }

    private static BigDecimal scoreBd(int score) {
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal weightBd(double weight) {
        return BigDecimal.valueOf(weight).setScale(2, RoundingMode.HALF_UP);
    }

    private static String gradeFromScore(int score) {
        if (score >= 90) {
            return "S";
        }
        if (score >= 80) {
            return "A";
        }
        if (score >= 70) {
            return "B";
        }
        if (score >= 55) {
            return "C";
        }
        return "D";
    }

    private static void appendStep(StringBuilder log, int step, String msg) {
        log.append("[").append(step).append("] ").append(msg).append("\n");
    }
}
