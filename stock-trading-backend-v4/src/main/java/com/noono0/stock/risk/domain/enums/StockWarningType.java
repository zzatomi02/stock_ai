package com.noono0.stock.risk.domain.enums;

/**
 * KRX 투자유의·관리종목 등 경보 유형.
 *
 * <p>투자경고/위험/거래정지 등은 기본 매수 금지. 투자주의는 점수 가산(리스크 페널티).
 */
public enum StockWarningType {
    INVESTMENT_CAUTION("투자주의", false, 15),
    INVESTMENT_WARNING("투자경고", true, 40),
    INVESTMENT_RISK("투자위험", true, 50),
    ADMINISTRATIVE("관리종목", true, 35),
    TRADING_HALT("거래정지", true, 100),
    SHORT_TERM_OVERHEATED("단기과열", false, 20),
    VI_TRIGGERED("VI발동", false, 25),
    DISCLOSURE_FAILURE("불성실공시", true, 45),
    DELISTING_RISK("상장폐지위험", true, 60);

    private final String label;
    private final boolean defaultBuyBlocked;
    private final int defaultRiskScorePenalty;

    StockWarningType(String label, boolean defaultBuyBlocked, int defaultRiskScorePenalty) {
        this.label = label;
        this.defaultBuyBlocked = defaultBuyBlocked;
        this.defaultRiskScorePenalty = defaultRiskScorePenalty;
    }

    public String label() {
        return label;
    }

    public boolean defaultBuyBlocked() {
        return defaultBuyBlocked;
    }

    public int defaultRiskScorePenalty() {
        return defaultRiskScorePenalty;
    }
}
