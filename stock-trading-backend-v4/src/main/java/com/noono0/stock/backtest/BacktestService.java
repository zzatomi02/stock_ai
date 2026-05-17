package com.noono0.stock.backtest;

import com.fasterxml.jackson.databind.JsonNode;
import com.noono0.stock.integration.kis.config.KisProperties;
import com.noono0.stock.integration.kis.service.KisBrokerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 과거 캔들·체결 DB 적재 전까지, KIS 현재가를 참고한 간이 시뮬레이션 결과를 반환합니다.
 */
@Service
@RequiredArgsConstructor
public class BacktestService {

    private final KisBrokerService kisBrokerService;
    private final KisProperties kisProperties;

    public Map<String, Object> replay(String stockCode, LocalDate from, LocalDate to) {
        long days = Math.max(1, ChronoUnit.DAYS.between(from, to));
        double refPrice = 50_000;
        if (kisKeysPresent()) {
            JsonNode j = kisBrokerService.inquirePriceForConfig(stockCode);
            JsonNode out = j.path("output");
            String pr = out.path("stck_prpr").asText(null);
            if (StringUtils.hasText(pr)) {
                try {
                    refPrice = Double.parseDouble(pr.replace(",", "").trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        double seed = ((stockCode.hashCode() & 0x7fffffff) % 10_000) / 100_000.0;
        double simReturn = seed * (days / 30.0);
        double end = refPrice * (1 + simReturn);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stockCode", stockCode);
        m.put("from", from.toString());
        m.put("to", to.toString());
        m.put("calendarDays", days);
        m.put("referencePrice", refPrice);
        m.put("simulatedEndPrice", Math.round(end * 100) / 100.0);
        m.put("estimatedReturnRate", Math.round(simReturn * 10000) / 10000.0);
        m.put(
                "note",
                "간이 시뮬레이션입니다. 과거 구간 리플레이·캔들 엔진은 별도 데이터 적재 후 확장 예정입니다.");
        return m;
    }

    private boolean kisKeysPresent() {
        var c = kisProperties.selected();
        return StringUtils.hasText(c.getAppKey()) && StringUtils.hasText(c.getAppSecret());
    }
}
