package com.noono0.stock.ops.schedule;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 스케줄·배치 마지막 실행 시각 추적 */
@Component
public class ScheduleMonitor {

    private final ConcurrentHashMap<String, Instant> lastRuns = new ConcurrentHashMap<>();

    public void touch(String name) {
        lastRuns.put(name, Instant.now());
    }

    public Map<String, String> snapshot() {
        Map<String, String> m = new LinkedHashMap<>();
        lastRuns.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> m.put(e.getKey(), e.getValue().toString()));
        return Collections.unmodifiableMap(m);
    }
}
