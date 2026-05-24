package com.noono0.stock.disclosure.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.noono0.stock.disclosure.domain.StockDisclosure;
import com.noono0.stock.disclosure.repository.StockDisclosureRepository;
import com.noono0.stock.collect.util.DisclosureDuplicateHashUtil;
import com.noono0.stock.integration.dart.DartCorpCodeService;
import com.noono0.stock.integration.dart.DartOpenApiClient;
import com.noono0.stock.integration.dart.config.DartProperties;
import com.noono0.stock.strategy.service.TradingClockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisclosureCollectService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

    private final DartProperties dartProperties;
    private final DartOpenApiClient dartClient;
    private final DartCorpCodeService corpCodeService;
    private final StockDisclosureRepository repository;
    private final TradingClockService tradingClock;

    @Transactional
    public int collectRecent(String stockCode, int lookbackDays) {
        if (!dartProperties.isEnabled()) {
            return 0;
        }
        String normalized = DartCorpCodeService.normalizeStockCode(stockCode);
        Optional<String> corpOpt = corpCodeService.resolveCorpCode(normalized);
        if (corpOpt.isEmpty()) {
            log.warn("【DART】 corp_code 없음 stockCode={}", normalized);
            return 0;
        }
        LocalDate end = tradingClock.today();
        LocalDate begin = end.minusDays(Math.max(1, lookbackDays));
        String bgnDe = begin.format(YMD);
        String endDe = end.format(YMD);

        int saved = 0;
        int page = 1;
        int pageCount = Math.min(100, dartProperties.getMaxItemsPerStock());
        int maxItems = dartProperties.getMaxItemsPerStock();

        while (saved < maxItems) {
            JsonNode root =
                    dartClient.fetchDisclosureList(corpOpt.get(), bgnDe, endDe, page, pageCount);
            String status = root.path("status").asText("");
            if ("013".equals(status)) {
                break;
            }
            JsonNode list = root.path("list");
            if (!list.isArray() || list.isEmpty()) {
                break;
            }
            for (JsonNode item : list) {
                if (saved >= maxItems) {
                    break;
                }
                if (persistIfNew(normalized, corpOpt.get(), item)) {
                    saved++;
                }
            }
            int totalPage = root.path("total_page").asInt(1);
            if (page >= totalPage) {
                break;
            }
            page++;
        }
        log.info("【DART】 공시 수집 stock={} saved={} ({}~{})", normalized, saved, bgnDe, endDe);
        return saved;
    }

    private boolean persistIfNew(String stockCode, String corpCode, JsonNode item) {
        String rceptNo = item.path("rcept_no").asText("");
        if (!StringUtils.hasText(rceptNo)) {
            return false;
        }
        if (repository.existsByRceptNo(rceptNo)) {
            return false;
        }
        String dupHash = DisclosureDuplicateHashUtil.hash("DART", rceptNo, null, null);
        if (repository.existsByDuplicateHash(dupHash)) {
            return false;
        }
        String rceptDtStr = item.path("rcept_dt").asText("");
        LocalDate rceptDt;
        try {
            rceptDt = LocalDate.parse(rceptDtStr, YMD);
        } catch (Exception exception) {
            rceptDt = tradingClock.today();
        }
        StockDisclosure row = new StockDisclosure();
        row.setProvider("DART");
        row.setStockCode(stockCode);
        row.setCorpCode(corpCode);
        row.setRceptNo(rceptNo);
        row.setReportNm(item.path("report_nm").asText("").trim());
        row.setDisclosureType(item.path("report_nm").asText("").trim());
        row.setRceptDt(rceptDt);
        row.setSubmittedAt(rceptDt.atStartOfDay());
        row.setFlrNm(item.path("flr_nm").asText(null));
        row.setRm(item.path("rm").asText(null));
        row.setOriginalUrl("https://dart.fss.or.kr/dsaf001/main.do?rcpNo=" + rceptNo);
        row.setDuplicateHash(dupHash);
        row.setCollectedAt(LocalDateTime.now());
        repository.save(row);
        return true;
    }
}
