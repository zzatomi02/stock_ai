package com.noono0.stock.strategy.service;

import com.noono0.stock.strategy.dto.ScoreAdjustmentResult;
import com.noono0.stock.strategy.dto.StrategyComponentScores;
import com.noono0.stock.strategy.dto.StrategySelection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 시장·시간 가중치를 곱해 adjusted 점수를 계산한다.
 *
 * <p>finalWeightMultiplier = marketWeight × timeWeight
 */
@Slf4j
@Service
public class StrategyScoreAdjuster {

    public ScoreAdjustmentResult adjust(StrategyComponentScores components, StrategySelection selection) {
        if (!selection.executable() || selection.combinedWeightMultiplier() <= 0.001) {
            log.info(
                    "[SCORE-ADJUST] SKIP 가중치 적용 — 전략={} executable={} mult={}",
                    selection.strategyType(),
                    selection.executable(),
                    selection.combinedWeightMultiplier());
            return ScoreAdjustmentResult.compute(
                    components.compositeRawScore(), 0, 0);
        }
        ScoreAdjustmentResult result =
                ScoreAdjustmentResult.compute(
                        components.compositeRawScore(),
                        selection.marketWeightMultiplier(),
                        selection.timeWeightMultiplier());
        log.debug(
                "[SCORE-ADJUST] {} raw={} → adj={} ({})",
                selection.strategyType(),
                components.compositeRawScore(),
                result.adjustedFinalScore(),
                result.formulaSummary());
        return result;
    }

    public ScoreAdjustmentResult adjust(
            int rawScore, double marketWeight, double timeWeight) {
        return ScoreAdjustmentResult.compute(rawScore, marketWeight, timeWeight);
    }
}
