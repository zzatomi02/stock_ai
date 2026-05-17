package com.noono0.stock.strategy.domain;

/** 플랫폼 전략 카탈로그 코드 */
public enum StrategyCode {
    SUPPLY_SCALP("수급단타", "TRADE", true),
    CLOSE_BET("종가베팅", "TRADE", true),
    OPEN_BET("시가베팅", "TRADE", true),
    BREAKOUT("돌파매매", "TRADE", true),
    PULLBACK("눌림목매매", "TRADE", true),
    SHORT_SWING("단기스윙", "TRADE", true),
    RISK_LIQUIDATION("위험청산", "RISK", false),
    NEWS_THEME("뉴스/테마 분석", "ANALYSIS", false),
    PAPER_TRADE("모의매매", "EXECUTION", false),
    ORDER_REASON("매수·매도 근거 저장", "INFRA", false);

    private final String label;
    private final String category;
    /** 기본 매수 주문 전략 여부 (위험청산·분석 등은 false) */
    private final boolean buyStrategy;

    StrategyCode(String label, String category, boolean buyStrategy) {
        this.label = label;
        this.category = category;
        this.buyStrategy = buyStrategy;
    }

    public String label() {
        return label;
    }

    public String category() {
        return category;
    }

    public boolean buyStrategy() {
        return buyStrategy;
    }
}
