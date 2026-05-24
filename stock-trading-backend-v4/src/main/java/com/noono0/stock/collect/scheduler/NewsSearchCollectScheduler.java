package com.noono0.stock.collect.scheduler;

import com.noono0.stock.collect.domain.NewsSearchKeyword;
import com.noono0.stock.collect.service.NaverNewsCollectorService;
import com.noono0.stock.collect.service.NewsSearchKeywordService;
import com.noono0.stock.ops.schedule.ScheduleMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.collect.news-search-enabled", havingValue = "true", matchIfMissing = false)
public class NewsSearchCollectScheduler {
    private final NewsSearchKeywordService keywordService;
    private final NaverNewsCollectorService naverNewsCollectorService;
    private final ScheduleMonitor scheduleMonitor;

    @Scheduled(fixedDelayString = "${app.collect.news-search-fixed-delay-ms:60000}")
    public void collectDueKeywords() {
        List<NewsSearchKeyword> due = keywordService.findDueKeywords();
        if (due.isEmpty()) {
            return;
        }
        int totalSaved = 0;
        for (NewsSearchKeyword keyword : due) {
            try {
                totalSaved += naverNewsCollectorService.collectByKeyword(keyword);
            } catch (Exception exception) {
                log.warn("뉴스 키워드 수집 실패 id={} keyword={}: {}", keyword.getId(), keyword.getKeyword(), exception.getMessage());
            }
        }
        scheduleMonitor.touch("news-search-collect");
        log.debug("뉴스 검색 수집 배치 완료 keywords={} saved={}", due.size(), totalSaved);
    }
}
