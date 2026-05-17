package com.noono0.stock.strategy.service;

import com.noono0.stock.ai.platform.service.CachedAiAnalysisQueryService;
import com.noono0.stock.ai.platform.service.StrategyAiPolicyService;
import com.noono0.stock.news.domain.NewsArticle;
import com.noono0.stock.strategy.domain.StrategyMaster;
import com.noono0.stock.strategy.domain.enums.MarketCondition;
import com.noono0.stock.strategy.domain.enums.MarketTimeWindow;
import com.noono0.stock.strategy.domain.enums.RecommendedAction;
import com.noono0.stock.strategy.domain.enums.SignalType;
import com.noono0.stock.strategy.domain.enums.StrategyType;
import com.noono0.stock.strategy.dto.*;
import com.noono0.stock.strategy.repository.StrategyMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 최종 매매 판단 오케스트레이션.
 *
 * <pre>
 * 전략 기본 + 오늘 임시 + 시장별 + 시간대별 → 실행 가능 여부
 * 뉴스 + 수급 + 기술 − 리스크 → raw
 * × (시장가중치 × 시간가중치) → adjusted
 * → 매수/매도 신호
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyDecisionEngine {

    private static final int DEFAULT_MIN_BUY = 62;

    private final TradingClockService tradingClock;
    private final MarketConditionAnalyzer marketConditionAnalyzer;
    private final MarketTimeWindowResolver marketTimeWindowResolver;
    private final StrategyMasterRepository masterRepository;
    private final StrategySelector strategySelector;
    private final StrategyComponentScorer componentScorer;
    private final StrategyScoreAdjuster scoreAdjuster;
    private final CachedAiAnalysisQueryService cachedAiQuery;
    private final StrategyAiPolicyService strategyAiPolicy;

    public TradingContextResponse evaluateContext() {
        return evaluateContext(null);
    }

    public TradingContextResponse evaluateContext(NewsArticle article) {
        LocalDate tradeDate = tradingClock.today();
        var timeRes = marketTimeWindowResolver.resolve();
        MarketConditionAnalysis market = marketConditionAnalyzer.analyze();

        List<StrategyDecisionDto> decisions = new ArrayList<>();
        for (StrategyMaster master : masterRepository.findAllByOrderBySortOrderAscIdAsc()) {
            decisions.add(decide(master, tradeDate, market, timeRes.window(), article));
        }

        return new TradingContextResponse(
                tradeDate.toString(),
                tradingClock.now().withNano(0).toString(),
                market.marketScore(),
                market.condition(),
                market.conditionLabel(),
                timeRes.window(),
                timeRes.label(),
                TradingContextResponse.FORMULA,
                decisions);
    }

    public StrategyDecisionDto decideForStrategy(String strategyType, NewsArticle article) {
        StrategyMaster master =
                masterRepository
                        .findByStrategyType(strategyType)
                        .orElseThrow(() -> new IllegalArgumentException("전략 없음: " + strategyType));
        var market = marketConditionAnalyzer.analyze();
        var window = marketTimeWindowResolver.resolve().window();
        return decide(master, tradingClock.today(), market, window, article);
    }

    public StrategyDecisionDto decide(
            StrategyMaster master,
            LocalDate tradeDate,
            MarketConditionAnalysis market,
            MarketTimeWindow window,
            NewsArticle article) {

        StrategySelection selection =
                strategySelector.select(master, tradeDate, market.condition(), window);

        boolean masterEnabled = selection.masterEnabled();
        boolean todayAllows = selection.todayRuntimeEnabled();
        boolean marketAllows = selection.marketRuleEnabled();
        boolean timeAllows = selection.timeWindowEnabled();
        boolean executable = selection.executable() && selection.combinedWeightMultiplier() > 0.01;

        String excludedReason = null;
        if (!executable) {
            excludedReason = buildExcludedReason(selection, window);
            log.info(
                    "[STRATEGY-DECISION] {} 비활성 — {}",
                    master.getStrategyType(),
                    excludedReason);
        }

        StrategyComponentScores components = null;
        ScoreAdjustmentResult adjustment = null;
        boolean scoresPass = false;
        boolean rationaleOk = false;
        String side = null;

        if (executable && article != null) {
            side = resolveSide(master.getStrategyType(), article);
            if (side != null) {
                components = componentScorer.score(article, side);
                adjustment = scoreAdjuster.adjust(components, selection);
                int min = resolveMinThreshold(master.getStrategyType(), side);
                int adj = adjustment.adjustedFinalScore().intValue();
                scoresPass = passesMin(side, adj, min);
                rationaleOk = hasRationale(article, side);
            }
        }

        boolean finalAllowed = executable && side != null && scoresPass && rationaleOk;
        RecommendedAction action = mapAction(finalAllowed, side, executable);

        if (finalAllowed
                && "BUY".equals(side)
                && article != null
                && StringUtils.hasText(article.getStockCode())) {
            var ai = cachedAiQuery.getBestForTrading(article.getStockCode());
            if (ai.isPresent() && strategyAiPolicy.isAiAvoidDecision(ai.get().decision())) {
                finalAllowed = false;
                action = RecommendedAction.WATCH_ONLY;
            }
        }

        SignalType signalType = mapSignalType(side, finalAllowed);

        String summary = buildSummary(executable, finalAllowed, excludedReason, adjustment);

        return new StrategyDecisionDto(
                master.getStrategyType(),
                master.getStrategyName(),
                market.condition(),
                window,
                masterEnabled,
                todayAllows,
                marketAllows,
                timeAllows,
                executable,
                scoresPass,
                rationaleOk,
                finalAllowed,
                signalType,
                action,
                components,
                adjustment,
                selection.marketWeightMultiplier(),
                selection.timeWeightMultiplier(),
                selection.combinedWeightMultiplier(),
                side != null ? resolveMinThreshold(master.getStrategyType(), side) : null,
                excludedReason,
                summary);
    }

    /** enabled-now API 호환 */
    public EnabledNowResponse toEnabledNowResponse(TradingContextResponse ctx) {
        List<EnabledStrategyItemDto> items = new ArrayList<>();
        for (StrategyDecisionDto d : ctx.strategies()) {
            if (d.strategyExecutable()) {
                double mw = d.marketWeightMultiplier();
                double tw = d.timeWeightMultiplier();
                double fw = d.finalWeightMultiplier();
                items.add(
                        EnabledStrategyItemDto.enabled(
                                d.strategyType(),
                                mw,
                                tw,
                                fw,
                                d.minScoreThreshold(),
                                d.decisionSummary()));
            } else {
                items.add(EnabledStrategyItemDto.disabled(d.strategyType(), d.excludedReason()));
            }
        }
        return new EnabledNowResponse(
                ctx.tradeDate(),
                ctx.currentTime(),
                ctx.marketCondition() != null ? ctx.marketCondition().name() : "",
                ctx.marketTimeWindow() != null ? ctx.marketTimeWindow().name() : "",
                items);
    }

    private static String buildExcludedReason(StrategySelection selection, MarketTimeWindow window) {
        if (!selection.failedChecks().isEmpty()) {
            return switch (selection.failedChecks().get(0)) {
                case "strategy_master" -> "전략 마스터에서 비활성화됨";
                case "strategy_runtime_setting" -> "오늘 임시 설정으로 OFF";
                case "market_condition_strategy" -> "현재 시장 상태에서 비활성화됨";
                case "strategy_time_window" -> "현재 시간대에서 비활성화된 전략 (" + window.label() + ")";
                default -> selection.reasonSummary();
            };
        }
        if (selection.combinedWeightMultiplier() <= 0.01) {
            return "가중치 0으로 비활성화됨";
        }
        return selection.reasonSummary();
    }

    private static String buildSummary(
            boolean executable, boolean finalAllowed, String excluded, ScoreAdjustmentResult adj) {
        if (finalAllowed && adj != null) {
            return "실행 허용 — adjusted=" + adj.adjustedFinalScore();
        }
        if (!executable) {
            return excluded != null ? excluded : "전략 비활성";
        }
        return "실행 불가 — 점수·근거 미충족";
    }

    private static boolean hasRationale(NewsArticle article, String side) {
        if ("BUY".equals(side) || "SELL".equals(side)) {
            return StringUtils.hasText(article.getLlmActionHint())
                    || StringUtils.hasText(article.getLlmSentiment())
                    || StringUtils.hasText(article.getTitle());
        }
        return true;
    }

    private static SignalType mapSignalType(String side, boolean allowed) {
        if (!allowed || side == null) {
            return SignalType.AVOID;
        }
        return switch (side) {
            case "BUY" -> SignalType.BUY;
            case "SELL" -> SignalType.SELL;
            default -> SignalType.WATCH;
        };
    }

    private static RecommendedAction mapAction(boolean finalAllowed, String side, boolean executable) {
        if (!executable || !finalAllowed) {
            return RecommendedAction.NO_ACTION;
        }
        if ("BUY".equals(side)) {
            return RecommendedAction.BUY_NOW;
        }
        if ("SELL".equals(side)) {
            return RecommendedAction.SELL_ALL;
        }
        return RecommendedAction.WATCH_ONLY;
    }

    private String resolveSide(String strategyType, NewsArticle article) {
        try {
            return switch (StrategyType.valueOf(strategyType)) {
                case RISK_EXIT -> "SELL";
                case NEWS_THEME -> "ANALYSIS";
                default -> inferSide(article);
            };
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String inferSide(NewsArticle article) {
        if (StringUtils.hasText(article.getLlmActionHint())) {
            if ("BUY".equalsIgnoreCase(article.getLlmActionHint())) return "BUY";
            if ("SELL".equalsIgnoreCase(article.getLlmActionHint())) return "SELL";
        }
        if ("POSITIVE".equalsIgnoreCase(article.getLlmSentiment())) return "BUY";
        if ("NEGATIVE".equalsIgnoreCase(article.getLlmSentiment())) return "SELL";
        return null;
    }

    private static int resolveMinThreshold(String strategyType, String side) {
        if ("SELL".equals(side)) return 38;
        if ("ANALYSIS".equals(side)) return 50;
        return DEFAULT_MIN_BUY;
    }

    private static boolean passesMin(String side, int adjusted, int min) {
        if ("SELL".equals(side)) return adjusted <= min;
        return adjusted >= min;
    }
}
