package com.noono0.stock.strategy.domain.enums;

/** 전략 마스터 코드 */
public enum StrategyType {
    SUPPLY_SCALPING("수급단타", true),
    CLOSING_BET("종가베팅", true),
    OPENING_BET("시가베팅", true),
    BREAKOUT("돌파매매", true),
    PULLBACK("눌림목", true),
    SHORT_SWING("단기스윙", true),
    NEWS_THEME("뉴스/테마", false),
    RISK_EXIT("위험청산", false);

    private final String label;
    private final boolean buyStrategy;

    StrategyType(String label, boolean buyStrategy) {
        this.label = label;
        this.buyStrategy = buyStrategy;
    }

    public String label() {
        return label;
    }

    public boolean buyStrategy() {
        return buyStrategy;
    }
}
