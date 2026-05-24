package com.noono0.stock.execution;

/**
 * 플랫폼 실행 단계. KIS paper/real({@code X-Trading-Mode})와 별개.
 *
 * <ul>
 *   <li>OBSERVE — 분석·종목 추천 목록만 (주문·승인 알림 없음)
 *   <li>PAPER_ALERT — 모의 + 추천 알림 → 사람 승인 후 주문
 *   <li>PAPER_AUTO — 모의 + 조건 충족 시 자동 주문
 *   <li>REAL_ALERT — 실전 + 추천 알림 → 사람 승인 후 주문
 *   <li>REAL_AUTO — 실전 + 자동 주문
 * </ul>
 */
public enum ExecutionPhase {
    OBSERVE,
    /** @deprecated {@link #PAPER_ALERT} */
    VIRTUAL,
    /** @deprecated {@link #PAPER_ALERT} */
    KIS_PAPER,
    PAPER_ALERT,
    PAPER_AUTO,
    /** @deprecated {@link #REAL_ALERT} */
    REAL_MANUAL,
    REAL_ALERT,
    REAL_AUTO;

    public static ExecutionPhase fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return OBSERVE;
        }
        String u = raw.trim().toUpperCase();
        return switch (u) {
            case "VIRTUAL" -> OBSERVE;
            case "KIS_PAPER" -> PAPER_ALERT;
            case "REAL_MANUAL" -> REAL_ALERT;
            default -> {
                try {
                    yield valueOf(u);
                } catch (IllegalArgumentException illegalArgumentException) {
                    yield OBSERVE;
                }
            }
        };
    }

    public boolean isObserveOnly() {
        return this == OBSERVE || this == VIRTUAL;
    }

    public boolean requiresHumanApproval() {
        return this == PAPER_ALERT || this == REAL_ALERT;
    }

    public boolean allowsAutoOrder() {
        return this == PAPER_AUTO || this == REAL_AUTO;
    }

    public String kisMode() {
        return (this == REAL_ALERT || this == REAL_AUTO) ? "real" : "paper";
    }

    public String labelKo() {
        return switch (this) {
            case OBSERVE, VIRTUAL -> "관찰 — 분석·종목 추천만";
            case PAPER_ALERT, KIS_PAPER -> "모의 — 추천 알림 + 승인 후 주문";
            case PAPER_AUTO -> "모의 — 자동매매";
            case REAL_ALERT, REAL_MANUAL -> "실전 — 추천 알림 + 승인 후 주문";
            case REAL_AUTO -> "실전 — 자동매매";
        };
    }
}
