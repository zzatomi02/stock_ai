package com.noono0.stock.recommendation.scheduler;

import com.noono0.stock.execution.runtime.ExecutionRuntimeService;
import com.noono0.stock.ops.schedule.ScheduleMonitor;
import com.noono0.stock.recommendation.service.StockRecommendationAutoExecutor;
import com.noono0.stock.recommendation.service.StockRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockRecommendationScheduler {

    private final StockRecommendationService recommendationService;
    private final StockRecommendationAutoExecutor autoExecutor;
    private final ExecutionRuntimeService executionRuntimeService;
    private final ScheduleMonitor scheduleMonitor;

    @Scheduled(fixedDelayString = "${app.recommendation.sync-interval-ms:180000}")
    public void syncTick() {
        scheduleMonitor.touch("stock-recommendation-sync");
        try {
            recommendationService.syncFromStrategySignals();
            if (executionRuntimeService.get().isAutoOrderScanEnabled()) {
                autoExecutor.scanAndOrder();
            }
        } catch (Exception exception) {
            log.warn("【REC-SCHED】 {}", exception.getMessage());
        }
    }
}
