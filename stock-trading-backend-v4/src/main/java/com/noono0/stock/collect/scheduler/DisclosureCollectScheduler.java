package com.noono0.stock.collect.scheduler;

import com.noono0.stock.collect.config.CollectProperties;
import com.noono0.stock.collect.service.KindRssDisclosureCollectorService;
import com.noono0.stock.ops.schedule.ScheduleMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.collect.kind-rss-enabled", havingValue = "true", matchIfMissing = false)
public class DisclosureCollectScheduler {
    private final KindRssDisclosureCollectorService kindRssDisclosureCollectorService;
    private final CollectProperties collectProperties;
    private final ScheduleMonitor scheduleMonitor;

    @Scheduled(cron = "${app.collect.kind-rss-cron:0 */30 * * * *}")
    public void collectKindRss() {
        if (!collectProperties.isKindRssEnabled()) {
            return;
        }
        try {
            int n = kindRssDisclosureCollectorService.collect();
            scheduleMonitor.touch("kind-rss-collect");
            log.info("KIND RSS 수집 완료 saved={}", n);
        } catch (Exception exception) {
            log.warn("KIND RSS 수집 실패: {}", exception.getMessage());
        }
    }
}
