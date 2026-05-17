package com.noono0.stock.strategy.domain;

import java.time.LocalTime;

/** 장중 시간대 슬롯 (KST) */
public enum TradingTimeSlot {
    PRE_OPEN_OBSERVE("0900_0903", "09:00~09:03 관찰", LocalTime.of(9, 0), LocalTime.of(9, 3)),
    OPEN_BET_WINDOW("0903_0920", "09:03~09:20 시가", LocalTime.of(9, 3), LocalTime.of(9, 20)),
    MORNING("0920_1130", "09:20~11:30 오전", LocalTime.of(9, 20), LocalTime.of(11, 30)),
    LUNCH("1130_1300", "11:30~13:00 점심", LocalTime.of(11, 30), LocalTime.of(13, 0)),
    AFTERNOON("1300_1430", "13:00~14:30 오후", LocalTime.of(13, 0), LocalTime.of(14, 30)),
    CLOSE_BET_WINDOW("1430_1520", "14:30~15:20 종가", LocalTime.of(14, 30), LocalTime.of(15, 20)),
    CLOSING("1520_1530", "15:20~15:30 마감", LocalTime.of(15, 20), LocalTime.of(15, 30));

    private final String code;
    private final String label;
    private final LocalTime start;
    private final LocalTime end;

    TradingTimeSlot(String code, String label, LocalTime start, LocalTime end) {
        this.code = code;
        this.label = label;
        this.start = start;
        this.end = end;
    }

    public String code() {
        return code;
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

    public static TradingTimeSlot currentSlot(LocalTime t) {
        for (TradingTimeSlot slot : values()) {
            if (slot.contains(t)) {
                return slot;
            }
        }
        return null;
    }
}
