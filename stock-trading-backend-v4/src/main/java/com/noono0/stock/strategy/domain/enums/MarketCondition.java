package com.noono0.stock.strategy.domain.enums;

/** 시장 상태 */
public enum MarketCondition {
    STRONG_BULL("강한 상승장"),
    BULL("상승장"),
    SIDEWAYS("횡보장"),
    WEAK("약한 장"),
    BEAR("하락장"),
    THEME("테마장"),
    LARGE_CAP("대형주 장세"),
    SMALL_CAP("개별주 장세"),
    HIGH_VOLATILITY("변동성 큰 장");

    private final String label;

    MarketCondition(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** @deprecated {@link com.noono0.stock.strategy.service.MarketConditionAnalyzer#fromMarketScore} 사용 */
    @Deprecated
    public static MarketCondition fromMoodScore(int score) {
        return com.noono0.stock.strategy.service.MarketConditionAnalyzer.fromMarketScore(score);
    }
}
