package com.noono0.stock.collect.service;

import com.noono0.stock.collect.domain.CollectJobLog;
import com.noono0.stock.collect.enums.CollectJobStatus;
import com.noono0.stock.disclosure.service.DisclosureCollectService;
import com.noono0.stock.integration.dart.config.DartProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DartDisclosureCollectorService {
    private static final String JOB_NAME = "dart-disclosure";
    private static final String PROVIDER = "DART";

    private final DisclosureCollectService disclosureCollectService;
    private final CollectLogService collectLogService;
    private final DartProperties dartProperties;

    public int collectForStock(String stockCode, int lookbackDays) {
        if (!dartProperties.isEnabled()) {
            return 0;
        }
        CollectJobLog job =
                collectLogService.startJob(JOB_NAME, PROVIDER, "STOCK_CODE", stockCode);
        int errors = 0;
        int saved = 0;
        try {
            saved = disclosureCollectService.collectRecent(stockCode, lookbackDays);
            collectLogService.finishJob(
                    job, 1, saved, saved, 0, 0, 0, CollectJobStatus.SUCCESS, "lookbackDays=" + lookbackDays);
        } catch (Exception exception) {
            errors = 1;
            collectLogService.logError(
                    JOB_NAME, PROVIDER, "STOCK_CODE", stockCode, exception.getClass().getSimpleName(), exception.getMessage(), exception, null, null);
            collectLogService.finishJob(job, 1, 0, 0, 0, 0, errors, CollectJobStatus.FAILED, exception.getMessage());
            log.warn("DART 공시 수집 실패 stock={}: {}", stockCode, exception.getMessage());
        }
        return saved;
    }
}
