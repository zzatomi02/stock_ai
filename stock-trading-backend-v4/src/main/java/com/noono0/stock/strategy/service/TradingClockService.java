package com.noono0.stock.strategy.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 평가 시각 제공. 테스트·시뮬레이션 시 {@link #runAt(LocalDateTime, Runnable)} 로 09:01 등 시간 고정 가능.
 */
@Service
public class TradingClockService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final ThreadLocal<LocalDateTime> OVERRIDE = new ThreadLocal<>();

    public LocalDateTime now() {
        LocalDateTime o = OVERRIDE.get();
        return o != null ? o : LocalDateTime.now(KST);
    }

    public LocalDate today() {
        return now().toLocalDate();
    }

    public LocalTime currentTime() {
        return now().toLocalTime();
    }

    public void setOverride(LocalDateTime at) {
        if (at == null) {
            OVERRIDE.remove();
        } else {
            OVERRIDE.set(at.atZone(KST).toLocalDateTime());
        }
    }

    public void clearOverride() {
        OVERRIDE.remove();
    }

    public void runAt(LocalDateTime at, Runnable action) {
        runAt(at, () -> {
            action.run();
            return null;
        });
    }

    public <T> T runAt(LocalDateTime at, java.util.function.Supplier<T> action) {
        LocalDateTime prev = OVERRIDE.get();
        try {
            setOverride(at);
            return action.get();
        } finally {
            if (prev == null) {
                clearOverride();
            } else {
                OVERRIDE.set(prev);
            }
        }
    }
}
