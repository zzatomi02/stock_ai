package com.noono0.stock.strategy.service;

import com.noono0.stock.ai.platform.service.AiScoreBlendService;
import com.noono0.stock.ai.platform.service.ClosingBetFastAiService;
import com.noono0.stock.ai.platform.service.CachedAiAnalysisQueryService;
import com.noono0.stock.strategy.domain.StrategySignal;
import com.noono0.stock.strategy.dto.StrategyComponentScores;
import com.noono0.stock.strategy.dto.StrategyDecisionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 장중 시그널 저장 시 Rule + Risk + AI 캐시 반영.
 */
@Service
@RequiredArgsConstructor
public class StrategySignalEnrichmentService {

    private final AiScoreBlendService aiScoreBlendService;
    private final CachedAiAnalysisQueryService cachedAiQuery;
    private final ClosingBetFastAiService closingBetFastAiService;
    private final StrategySignalRiskGateService riskGate;

    public record EnrichmentResult(
            int ruleBasedScore,
            int finalBlendedScore,
            boolean riskPassed,
            String riskBlockReason,
            boolean downgradedByAi) {}

    public EnrichmentResult enrich(
            StrategySignal sig,
            StrategyDecisionDto decision,
            StrategyComponentScores components,
            String side) {
        int ruleBased =
                components != null ? components.compositeRawScore() : 0;
        sig.setRuleBasedScore(ruleBased);

        var risk = riskGate.check(sig.getStockCode(), sig.getStrategyCode(), side);
        sig.setRiskCheckPassed(risk.passed());
        sig.setRiskBlockReason(risk.blockReason());

        closingBetFastAiService.tryRefreshCacheIfNeeded(
                sig.getStockCode(), sig.getStockName(), sig.getStrategyCode());

        var blend = aiScoreBlendService.blend(ruleBased, sig.getStockCode(), sig.getStrategyCode());
        sig.setAiOverallScore(bd(blend.aiOverallScore()));
        sig.setAiBlendedScore(bd(blend.finalBuyScore()));
        sig.setAiDecision(blend.aiDecision());
        if (blend.aiUsed()) {
            cachedAiQuery.getBestForTrading(sig.getStockCode()).ifPresent(c -> {
                sig.setAiCompanyAnalysisId(c.id());
                sig.setAiBuyScore(c.buyScore());
                sig.setAiRiskScore(c.riskScore());
                sig.setAiSummary(c.summary());
            });
        }

        boolean downgraded = false;
        int finalScore = blend.finalBuyScore();
        if (!risk.passed()) {
            finalScore = Math.min(finalScore, ruleBased);
        }
        if (blend.aiBlockedAction()) {
            downgraded = true;
            if (StringUtils.hasText(blend.aiRiskReason())) {
                sig.setAiSummary(blend.aiRiskReason());
            }
        }

        sig.setAdjustedFinalScore(bd(finalScore));
        return new EnrichmentResult(ruleBased, finalScore, risk.passed(), risk.blockReason(), downgraded);
    }

    private static BigDecimal bd(int v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal bd(BigDecimal v) {
        return v != null ? v.setScale(2, RoundingMode.HALF_UP) : null;
    }
}
