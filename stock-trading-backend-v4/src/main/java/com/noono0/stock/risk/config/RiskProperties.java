package com.noono0.stock.risk.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.risk")
public class RiskProperties {

    /** 일일 최대 손실률(%) — 도달 시 신규 매수 중지 */
    private double dailyMaxLossRatePercent = 3.0;

    /** 일일 최대 손실금액(원) */
    private long dailyMaxLossAmount = 500_000L;

    /** 기준 총자산(원) — 손실률·종목 비중 계산용 */
    private long referenceEquity = 10_000_000L;

    /** 종목당 최대 매수 비중(총자산 대비 %) */
    private double maxPositionPerSymbolPercent = 10.0;

    /** 전략당 최대 매수금액(원) */
    private long maxPositionPerStrategyAmount = 2_000_000L;

    /** 하루 최대 매매(매수) 횟수 */
    private int dailyMaxBuyCount = 50;

    /** 동일 종목 재진입 제한(분) */
    private int reentryCooldownMinutes = 30;

    /** 연속 손절 N회 시 해당 전략 오늘 OFF */
    private int consecutiveStopLossLimit = 3;

    /** 시장 급락(분위기 점수 이하) 시 신규 매수 중지 */
    private int marketCrashMoodThreshold = 20;

    /** 1회 주문 최대 금액(원) */
    private long singleOrderMaxAmount = 10_000_000L;

    /** KIS/API 장애 시 주문 차단 */
    private boolean haltOnApiError = true;

    /** 시세 지연 허용(ms) — 초과 시 주문 차단 */
    private long quoteDelayMaxMs = 5_000L;

    private List<TimeWindowTradeLimit> timeWindowLimits = defaultTimeWindows();

    private static List<TimeWindowTradeLimit> defaultTimeWindows() {
        List<TimeWindowTradeLimit> list = new ArrayList<>();
        TimeWindowTradeLimit w = new TimeWindowTradeLimit();
        w.setStart("09:00");
        w.setEnd("09:10");
        w.setMaxTrades(3);
        return list;
    }

    @Getter
    @Setter
    public static class TimeWindowTradeLimit {
        private String start = "09:00";
        private String end = "09:10";
        private int maxTrades = 3;
    }

    public BigDecimal maxPositionPerSymbolAmount() {
        return BigDecimal.valueOf(referenceEquity)
                .multiply(BigDecimal.valueOf(maxPositionPerSymbolPercent / 100.0));
    }
}
