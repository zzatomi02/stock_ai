package com.noono0.stock.strategy.domain.enums;

import java.time.LocalTime;

/** 장중 시간대 (KST) */
public enum MarketTimeWindow {
    PRE_MARKET("장 시작 전", LocalTime.MIN, LocalTime.of(9, 0)),
    OPENING_NO_TRADE("09:00~09:03 관찰", LocalTime.of(9, 0), LocalTime.of(9, 3)),
    OPENING_BET("09:03~09:20 시가베팅", LocalTime.of(9, 3), LocalTime.of(9, 20)),
    EARLY_MARKET("09:20~10:30 장초반", LocalTime.of(9, 20), LocalTime.of(10, 30)),
    MORNING_MARKET("10:30~11:30 오전장", LocalTime.of(10, 30), LocalTime.of(11, 30)),
    LUNCH_MARKET("11:30~13:00 점심장", LocalTime.of(11, 30), LocalTime.of(13, 0)),
    AFTERNOON_MARKET("13:00~14:30 오후장", LocalTime.of(13, 0), LocalTime.of(14, 30)),
    CLOSING_PREPARE("14:30~15:10 종가 후보", LocalTime.of(14, 30), LocalTime.of(15, 10)),
    CLOSING_BET("15:10~15:20 종가베팅", LocalTime.of(15, 10), LocalTime.of(15, 20)),
    AFTER_MARKET("장 마감 후", LocalTime.of(15, 20), LocalTime.MAX);

    private final String label;
    private final LocalTime start;
    private final LocalTime end;

    MarketTimeWindow(String label, LocalTime start, LocalTime end) {
        this.label = label;
        this.start = start;
        this.end = end;
    }

    public String label() {
        return label;
    }

    public LocalTime start() {
        return start;
    }

    public LocalTime end() {
        return end;
    }

    public boolean contains(LocalTime t) {
        return !t.isBefore(start) && t.isBefore(end);
    }

    public static MarketTimeWindow current(LocalTime t) {
        for (MarketTimeWindow w : values()) {
            if (w.contains(t)) {
                return w;
            }
        }
        return AFTER_MARKET;
    }
}
