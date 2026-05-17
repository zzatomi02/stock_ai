package com.noono0.stock.execution;

/**
 * 플랫폼 실행 단계. KIS paper/real({@code X-Trading-Mode})와 별개.
 */
public enum ExecutionPhase {
    /** 뉴스·GPT·매매 후보만, 주문 없음 */
    OBSERVE,
    /** DB 가상 주문만 (향후) */
    VIRTUAL,
    /** KIS 모의투자 API */
    KIS_PAPER,
    /** 실전 주문 — 사용자 승인 후 (향후) */
    REAL_MANUAL,
    /** 제한적 실전 자동매매 */
    REAL_AUTO;

    public static ExecutionPhase fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return OBSERVE;
        }
        try {
            return ExecutionPhase.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OBSERVE;
        }
    }
}
