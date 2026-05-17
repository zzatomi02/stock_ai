package com.noono0.stock.risk.dto;

/** 시장경보 필터 결과 */
public record WarningFilterResult(
        boolean buyAllowed,
        int riskScoreAdjustment,
        String riskReason,
        String warningType) {

    public static WarningFilterResult clear() {
        return new WarningFilterResult(true, 0, null, null);
    }

    public static WarningFilterResult blocked(String warningType, String riskReason, int scoreAdj) {
        return new WarningFilterResult(false, scoreAdj, riskReason, warningType);
    }

    public static WarningFilterResult caution(String warningType, String riskReason, int scoreAdj) {
        return new WarningFilterResult(true, scoreAdj, riskReason, warningType);
    }
}
