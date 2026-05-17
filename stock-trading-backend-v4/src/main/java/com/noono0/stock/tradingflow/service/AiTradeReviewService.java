package com.noono0.stock.tradingflow.service;

import com.noono0.stock.ai.platform.service.CachedAiAnalysisQueryService;
import com.noono0.stock.strategy.domain.StrategySignal;
import com.noono0.stock.strategy.repository.StrategySignalRepository;
import com.noono0.stock.strategy.service.TradingClockService;
import com.noono0.stock.tradingflow.domain.AiTradeReview;
import com.noono0.stock.tradingflow.repository.AiTradeReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTradeReviewService {

    private final TradingClockService tradingClock;
    private final StrategySignalRepository strategySignalRepository;
    private final AiTradeReviewRepository reviewRepository;
    private final CachedAiAnalysisQueryService cachedAiQuery;

    @Transactional
    public int reviewTodayCandidates() {
        var today = tradingClock.today();
        List<StrategySignal> signals =
                strategySignalRepository.findByTradeDateAndStatusOrderByAdjustedFinalScoreDesc(
                        today, "CANDIDATE");
        int created = 0;
        for (StrategySignal s : signals) {
            if (reviewRepository.findByTradeDateAndStrategySignalId(today, s.getId()).isPresent()) {
                continue;
            }
            AiTradeReview r = new AiTradeReview();
            r.setTradeDate(today);
            r.setStockCode(s.getStockCode());
            r.setStockName(s.getStockName());
            r.setStrategyCode(s.getStrategyCode());
            r.setStrategySignalId(s.getId());
            r.setFinalScore(
                    s.getAiBlendedScore() != null
                            ? s.getAiBlendedScore().intValue()
                            : (s.getAdjustedFinalScore() != null
                                    ? s.getAdjustedFinalScore().intValue()
                                    : 0));

            var ai = StringUtils.hasText(s.getStockCode()) ? cachedAiQuery.getBestForTrading(s.getStockCode()) : java.util.Optional.<com.noono0.stock.ai.platform.dto.CachedAiCompanyView>empty();
            if (ai.isPresent()) {
                var c = ai.get();
                r.setAiDecision(c.decision());
                r.setReviewSummary(
                        "전략="
                                + s.getStrategyCode()
                                + " side="
                                + s.getSide()
                                + " adj="
                                + s.getAdjustedFinalScore()
                                + " | AI: "
                                + (c.summary() != null ? c.summary() : c.decision()));
                r.setLessonsLearned(
                        s.getRiskBlockReason() != null
                                ? "리스크: " + s.getRiskBlockReason()
                                : (c.riskScore() != null ? "AI riskScore=" + c.riskScore() : null));
            } else {
                r.setReviewSummary(
                        "AI 분석 없음 — rule adj="
                                + s.getAdjustedFinalScore()
                                + " "
                                + (s.getReasonMessage() != null ? s.getReasonMessage() : ""));
            }
            reviewRepository.save(r);
            created++;
        }
        return created;
    }
}
