package com.noono0.stock.disclosure.web;

import com.noono0.stock.disclosure.domain.StockDisclosure;
import com.noono0.stock.disclosure.repository.StockDisclosureRepository;
import com.noono0.stock.disclosure.service.DisclosureCollectService;
import com.noono0.stock.integration.dart.DartCorpCodeService;
import com.noono0.stock.integration.dart.config.DartProperties;
import com.noono0.stock.strategy.service.TradingClockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disclosures")
@RequiredArgsConstructor
public class DisclosureController {

    private final DisclosureCollectService collectService;
    private final StockDisclosureRepository repository;
    private final TradingClockService tradingClock;
    private final DartProperties dartProperties;

    @GetMapping
    public List<StockDisclosure> list(
            @RequestParam String stockCode, @RequestParam(defaultValue = "7") int days) {
        String normalized = DartCorpCodeService.normalizeStockCode(stockCode);
        LocalDate since = tradingClock.today().minusDays(Math.max(1, days));
        return repository.findByStockCodeAndRceptDtGreaterThanEqualOrderByRceptDtDesc(
                normalized, since);
    }

    @PostMapping("/collect")
    public ResponseEntity<Map<String, Object>> collect(
            @RequestParam String stockCode, @RequestParam(required = false) Integer days) {
        int lookback = days != null ? days : dartProperties.getDefaultLookbackDays();
        int saved = collectService.collectRecent(stockCode, lookback);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stockCode", DartCorpCodeService.normalizeStockCode(stockCode));
        body.put("saved", saved);
        body.put("lookbackDays", lookback);
        return ResponseEntity.ok(body);
    }
}
