package com.noono0.stock.strategy.dto;

import java.math.BigDecimal;

public record ScoreAdjustmentResult(
        BigDecimal compositeRawScore,
        BigDecimal marketWeightMultiplier,
        BigDecimal timeWeightMultiplier,
        BigDecimal finalWeightMultiplier,
        BigDecimal adjustedFinalScore,
        String formulaSummary) {

    public static ScoreAdjustmentResult compute(int raw, double marketW, double timeW) {
        double mult = Math.round(marketW * timeW * 100.0) / 100.0;
        int adjusted = (int) Math.min(100, Math.round(raw * mult));
        return new ScoreAdjustmentResult(
                bd(raw),
                bd(marketW),
                bd(timeW),
                bd(mult),
                bd(adjusted),
                "adjusted = min(100, raw × 시장가중치 × 시간가중치) = min(100, "
                        + raw
                        + " × "
                        + marketW
                        + " × "
                        + timeW
                        + ") = "
                        + adjusted);
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
