package com.noono0.stock.collect.service;

import com.noono0.stock.collect.config.CollectProperties;
import com.noono0.stock.collect.domain.CollectJobLog;
import com.noono0.stock.collect.enums.CollectJobStatus;
import com.noono0.stock.collect.util.DisclosureDuplicateHashUtil;
import com.noono0.stock.disclosure.domain.StockDisclosure;
import com.noono0.stock.disclosure.repository.StockDisclosureRepository;
import com.noono0.stock.integration.kind.KindRssClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class KindRssDisclosureCollectorService {
    private static final String JOB_NAME = "kind-rss";
    private static final String PROVIDER = "KIND";

    private final KindRssClient kindRssClient;
    private final StockDisclosureRepository disclosureRepository;
    private final CollectLogService collectLogService;
    private final CollectProperties collectProperties;

    @Transactional
    public int collect() {
        if (!collectProperties.isKindRssEnabled()) {
            return 0;
        }
        String url = collectProperties.getKindRssUrl();
        CollectJobLog job = collectLogService.startJob(JOB_NAME, PROVIDER, "RSS", url);
        int saved = 0;
        int duplicate = 0;
        int skipped = 0;
        int errors = 0;
        int responseCount = 0;
        try {
            List<KindRssClient.KindRssItem> items = kindRssClient.fetch(url);
            responseCount = items.size();
            for (KindRssClient.KindRssItem item : items) {
                try {
                    switch (persist(item)) {
                        case SAVED -> saved++;
                        case DUPLICATE -> duplicate++;
                        case SKIPPED -> skipped++;
                    }
                } catch (Exception e) {
                    errors++;
                    collectLogService.logError(
                            JOB_NAME,
                            PROVIDER,
                            "RSS_ITEM",
                            item.title(),
                            e.getClass().getSimpleName(),
                            e.getMessage(),
                            e,
                            url,
                            null);
                }
            }
            CollectJobStatus status =
                    errors > 0
                            ? (saved > 0 ? CollectJobStatus.PARTIAL_SUCCESS : CollectJobStatus.FAILED)
                            : CollectJobStatus.SUCCESS;
            collectLogService.finishJob(
                    job, 1, responseCount, saved, duplicate, skipped, errors, status, null);
            return saved;
        } catch (Exception e) {
            collectLogService.logError(
                    JOB_NAME, PROVIDER, "RSS", url, e.getClass().getSimpleName(), e.getMessage(), e, url, null);
            collectLogService.finishJob(job, 1, 0, 0, 0, 0, 1, CollectJobStatus.FAILED, e.getMessage());
            throw e;
        }
    }

    private enum PersistResult {
        SAVED,
        DUPLICATE,
        SKIPPED
    }

    private PersistResult persist(KindRssClient.KindRssItem item) {
        if (!StringUtils.hasText(item.title())) {
            return PersistResult.SKIPPED;
        }
        String hash = DisclosureDuplicateHashUtil.hash(PROVIDER, null, item.link(), item.title());
        if (disclosureRepository.existsByDuplicateHash(hash)) {
            return PersistResult.DUPLICATE;
        }
        StockDisclosure row = new StockDisclosure();
        row.setProvider(PROVIDER);
        row.setReportNm(item.title().trim());
        row.setDisclosureType("MARKET_ACTION");
        row.setOriginalUrl(StringUtils.hasText(item.link()) ? item.link() : null);
        row.setSummary(item.description());
        row.setSubmittedAt(parsePub(item.pubDate()));
        row.setDuplicateHash(hash);
        row.setCollectedAt(LocalDateTime.now());
        disclosureRepository.save(row);
        return PersistResult.SAVED;
    }

    private static LocalDateTime parsePub(String pub) {
        if (!StringUtils.hasText(pub)) {
            return null;
        }
        try {
            return ZonedDateTime.parse(pub, DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH))
                    .toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }
}
