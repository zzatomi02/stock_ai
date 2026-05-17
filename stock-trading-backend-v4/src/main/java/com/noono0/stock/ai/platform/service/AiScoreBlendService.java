package com.noono0.stock.ai.platform.service;

import com.noono0.stock.ai.platform.config.AiPlatformProperties;
import com.noono0.stock.ai.platform.dto.CachedAiCompanyView;
import com.noono0.stock.strategy.domain.enums.RecommendedAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 빠른 매수 판단용 점수 블렌딩 — DB 캐시만 사용.
 *
 * <p>finalBuyScore = rule * wRule + aiOverall * wAi - riskPenalty
 */
@Service
@RequiredArgsConstructor
public class AiScoreBlendService {

    private final AiPlatformProperties properties;
    private final CachedAiAnalysisQueryService cachedAiQuery;
    private final StrategyAiPolicyService strategyAiPolicy;

    public record BlendResult(
            int ruleBasedScore,
            int aiOverallScore,
            int riskPenalty,
            int finalBuyScore,
            boolean aiUsed,
            boolean aiBlockedAction,
            String aiDecision,
            String aiRiskReason,
            RecommendedAction recommendedAction) {}

    public BlendResult blend(int ruleBasedScore, String stockCode, String strategyType) {
        int riskPenalty = 0;
        int aiOverall = 0;
        boolean aiUsed = false;
        String aiDecision = null;
        String aiRiskReason = null;
        boolean blocked = false;
        RecommendedAction action = null;

        Optional<CachedAiCompanyView> cached = cachedAiQuery.getBestForTrading(stockCode);
        if (cached.isPresent()) {
            var c = cached.get();
            aiUsed = true;
            if (c.overallScore() != null) {
                aiOverall = c.overallScore().intValue();
            }
            aiDecision = c.decision();
            if (c.riskScore() != null) {
                riskPenalty = Math.min(40, c.riskScore().intValue() / 2);
            }
            if (strategyAiPolicy.isAiAvoidDecision(aiDecision)) {
                blocked = true;
                aiRiskReason = c.summary();
                action = RecommendedAction.WATCH_ONLY;
            }
        } else {
            var fallback = strategyAiPolicy.policyFor(strategyType);
            action = fallback.whenNoAi();
        }

        double wRule = properties.getRuleScoreWeight();
        double wAi = properties.getAiScoreWeight();
        int finalScore =
                (int)
                        Math.round(
                                Math.max(
                                        0,
                                        Math.min(
                                                100,
                                                ruleBasedScore * wRule
                                                        + aiOverall * wAi
                                                        - riskPenalty)));

        if (!blocked && action == null) {
            action = finalScore >= 62 ? RecommendedAction.BUY_NOW : RecommendedAction.WATCH_ONLY;
        }

        return new BlendResult(
                ruleBasedScore, aiOverall, riskPenalty, finalScore, aiUsed, blocked, aiDecision, aiRiskReason, action);
    }
}
