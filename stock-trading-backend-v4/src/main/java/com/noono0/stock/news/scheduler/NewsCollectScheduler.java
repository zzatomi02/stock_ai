package com.noono0.stock.news.scheduler;

import com.noono0.stock.news.service.NewsSourceCollectService;
import com.noono0.stock.ops.schedule.ScheduleMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 네이버 뉴스 주기 수집 (NAVER 키 필요) */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.news.collect-enabled", havingValue = "true", matchIfMissing = false)
public class NewsCollectScheduler {
    private final NewsSourceCollectService newsSourceCollectService;
    private final ScheduleMonitor scheduleMonitor;

    @Scheduled(cron = "${app.news.collect-cron:0 0 */3 * * *}")
    public void collect() {
        try {
            int n = newsSourceCollectService.collectEnabledSources();
            scheduleMonitor.touch("news-collect");
            log.info("뉴스 소스 배치 수집 완료: {}건", n);
        } catch (Exception e) {
            log.warn("뉴스 소스 배치 수집 실패: {}", e.getMessage());
        }
    }
}
