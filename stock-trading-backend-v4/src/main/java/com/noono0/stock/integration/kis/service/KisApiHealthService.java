package com.noono0.stock.integration.kis.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** KIS/API 장애·시세 지연 상태 (인메모리). WebSocket·REST에서 갱신. */
@Service
public class KisApiHealthService {

    private final AtomicBoolean apiHealthy = new AtomicBoolean(true);
    private final AtomicLong lastQuoteAtMs = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong lastErrorAtMs = new AtomicLong(0);
    @Getter
    private volatile String lastErrorMessage;

    public void markQuoteReceived() {
        lastQuoteAtMs.set(System.currentTimeMillis());
    }

    public void markApiError(String message) {
        apiHealthy.set(false);
        lastErrorAtMs.set(System.currentTimeMillis());
        lastErrorMessage = message;
    }

    public void markApiRecovered() {
        apiHealthy.set(true);
        lastErrorMessage = null;
    }

    public boolean isApiHealthy() {
        return apiHealthy.get();
    }

    public long quoteDelayMs() {
        return System.currentTimeMillis() - lastQuoteAtMs.get();
    }

    public Instant lastQuoteInstant() {
        return Instant.ofEpochMilli(lastQuoteAtMs.get());
    }
}
