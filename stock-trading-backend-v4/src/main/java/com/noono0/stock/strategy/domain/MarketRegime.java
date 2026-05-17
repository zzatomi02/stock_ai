package com.noono0.stock.strategy.domain;

/** 시장 분위기 점수 구간 */
public enum MarketRegime {
    STRONG_BULL,
    BULL,
    NEUTRAL,
    BEAR,
    STRONG_BEAR;

    public static MarketRegime fromMoodScore(int score) {
        if (score >= 70) return STRONG_BULL;
        if (score >= 55) return BULL;
        if (score >= 45) return NEUTRAL;
        if (score >= 30) return BEAR;
        return STRONG_BEAR;
    }

    public String labelKo() {
        return switch (this) {
            case STRONG_BULL -> "강한 상승";
            case BULL -> "상승";
            case NEUTRAL -> "중립";
            case BEAR -> "하락";
            case STRONG_BEAR -> "강한 하락";
        };
    }
}
