package com.noono0.stock.market.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.noono0.stock.integration.kis.service.KisBrokerService;
import com.noono0.stock.market.dto.CandleDto;
import com.noono0.stock.market.dto.StockCandlesDto;
import com.noono0.stock.market.dto.StockDetailDto;
import com.noono0.stock.market.dto.Top100SnapshotDto;
import com.noono0.stock.market.dto.TopStockDto;
import com.noono0.stock.market.domain.MarketTop100Snapshot;
import com.noono0.stock.market.repository.MarketTop100SnapshotRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

    private final KisBrokerService kisBrokerService;
    private final MarketTop100SnapshotRepository marketTop100SnapshotRepository;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Object> refreshLocks = new ConcurrentHashMap<>();
    @Value("${app.market.top100-refresh-sec:30}")
    private int top100RefreshIntervalSec;

    /**
     * 한국투자 「거래량순위」 API + 연속조회(tr_cont)로 최대 100건.
     * 실패 시 더미 데이터를 반환하지 않고 실패 이유를 그대로 예외로 전달한다.
     */
    public Top100SnapshotDto getTop100Snapshot(String type, HttpServletRequest req) {
        String normalizedType = normalizeTopType(type);
        MarketTop100Snapshot snapshot = marketTop100SnapshotRepository.findByType(normalizedType).orElse(null);
        boolean needsRefresh = snapshot == null
                || snapshot.getUpdatedAt() == null
                || snapshot.getUpdatedAt().isBefore(LocalDateTime.now().minusSeconds(top100RefreshIntervalSec));

        if (needsRefresh) {
            Object lock = refreshLocks.computeIfAbsent(normalizedType, k -> new Object());
            synchronized (lock) {
                snapshot = marketTop100SnapshotRepository.findByType(normalizedType).orElse(snapshot);
                boolean stillNeeds = snapshot == null
                        || snapshot.getUpdatedAt() == null
                        || snapshot.getUpdatedAt().isBefore(LocalDateTime.now().minusSeconds(top100RefreshIntervalSec));
                if (stillNeeds) {
                    try {
                        List<TopStockDto> fresh = fetchTop100FromKis(normalizedType, req);
                        String payload = objectMapper.writeValueAsString(fresh);
                        MarketTop100Snapshot row = snapshot == null ? new MarketTop100Snapshot() : snapshot;
                        row.setType(normalizedType);
                        row.setPayloadJson(payload);
                        row.setUpdatedAt(LocalDateTime.now());
                        row.setLastError(null);
                        snapshot = marketTop100SnapshotRepository.save(row);
                        log.info("【KIS-TOP100-CACHE】 최신 스냅샷 저장 type={} count={} updatedAt={}",
                                normalizedType, fresh.size(), snapshot.getUpdatedAt());
                    } catch (Exception exception) {
                        log.warn("【KIS-TOP100-CACHE】 갱신 실패 type={} err={}", normalizedType, exception.getMessage());
                        if (snapshot == null || !StringUtils.hasText(snapshot.getPayloadJson())) {
                            throw exception instanceof RuntimeException ? (RuntimeException) exception : new IllegalStateException(exception.getMessage(), exception);
                        }
                        snapshot.setLastError(exception.getMessage());
                        marketTop100SnapshotRepository.save(snapshot);
                    }
                }
            }
        }

        List<TopStockDto> rows = parseSnapshotRows(snapshot);
        boolean stale = snapshot.getUpdatedAt() == null
                || snapshot.getUpdatedAt().isBefore(LocalDateTime.now().minusSeconds(top100RefreshIntervalSec));
        return new Top100SnapshotDto(
                normalizedType,
                rows,
                snapshot.getUpdatedAt() == null ? "" : snapshot.getUpdatedAt().toString(),
                stale,
                top100RefreshIntervalSec
        );
    }

    private List<TopStockDto> parseSnapshotRows(MarketTop100Snapshot snapshot) {
        if (snapshot == null || !StringUtils.hasText(snapshot.getPayloadJson())) return List.of();
        try {
            return objectMapper.readValue(snapshot.getPayloadJson(), new TypeReference<List<TopStockDto>>() {});
        } catch (Exception exception) {
            log.warn("【KIS-TOP100-CACHE】 스냅샷 역직렬화 실패 type={} err={}", snapshot.getType(), exception.getMessage());
            return List.of();
        }
    }

    private List<TopStockDto> fetchTop100FromKis(String type, HttpServletRequest req) {
        String normalizedType = normalizeTopType(type);
        String fidBlng = "amount".equalsIgnoreCase(normalizedType) ? "3" : "0";
        List<TopStockDto> out = new ArrayList<>();
        String reqTrCont = "";
        try {
            for (int page = 0; page < 8 && out.size() < 100; page++) {
                KisBrokerService.VolumeRankPage pg =
                        kisBrokerService.inquireVolumeRankPage(req, fidBlng, reqTrCont);
                JsonNode root = pg.body();
                if (root.has("error")) {
                    String msg = String.format(
                            "KIS Top100 호출 실패: error=%s, httpStatus=%s, phase=%s, responseBody=%s",
                            root.get("error").asText(),
                            root.path("httpStatus").asText(""),
                            root.path("phase").asText(""),
                            root.path("responseBody").asText(""));
                    log.warn("【KIS-TOP100】 {}", msg);
                    throw new IllegalStateException(msg);
                }

                String rt = root.path("rt_cd").asText("");
                if (!"0".equals(rt)) {
                    String msg = String.format(
                            "KIS Top100 API 비정상: rt_cd=%s, msg1=%s, msg2=%s",
                            rt,
                            root.path("msg1").asText(""),
                            root.path("msg2").asText(""));
                    log.warn("【KIS-TOP100】 {}", msg);
                    throw new IllegalStateException(msg);
                }

                JsonNode arr = root.path("output");
                if (!arr.isArray()) {
                    String msg = "KIS Top100 응답 오류: output 이 배열이 아님 (TR_ID/FID 파라미터 확인 필요)";
                    log.warn("【KIS-TOP100】 {}", msg);
                    throw new IllegalStateException(msg);
                }
                List<JsonNode> pageRows = new ArrayList<>();
                for (JsonNode row : arr) {
                    pageRows.add(row);
                }
                pageRows = sortRowsByType(pageRows, normalizedType);

                log.warn("【KIS-TOP100】 페이지 {} 수신 {}건 | 응답 tr_cont={}", page + 1, arr.size(), pg.responseTrCont());
                for (JsonNode row : pageRows) {
                    if (out.size() >= 100) {
                        break;
                    }
                    out.add(mapKisRankRow(row, normalizedType));
                }
                if (out.size() >= 100) {
                    break;
                }
                if (!"M".equalsIgnoreCase(pg.responseTrCont())) {
                    break;
                }
                reqTrCont = "N";
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("【KIS-TOP100】 예외: {}", exception.getMessage(), exception);
            throw new IllegalStateException("KIS Top100 조회 중 예외: " + exception.getMessage(), exception);
        }

        if (out.isEmpty()) {
            log.warn("【KIS-TOP100】 결과 0건");
            return List.of();
        }

        // type에 맞게 전체 100건 단위로 재정렬 + rank 재부여
        List<TopStockDto> sorted = sortDtosByType(out, normalizedType);
        List<TopStockDto> ranked = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            TopStockDto s = sorted.get(i);
            ranked.add(new TopStockDto(i + 1, s.stockCode(), s.stockName(), s.price(), s.changeRate(), s.volume(), s.newsScore(), s.aiScore(), s.finalScore()));
        }

        log.warn("【KIS-TOP100】 ★★ 완료 ★★ 총 {}건 (type={}, FID_BLNG_CLS_CODE={})", ranked.size(), normalizedType, fidBlng);
        return ranked;
    }

    private static String normalizeTopType(String type) {
        if ("rise".equalsIgnoreCase(type) || "up".equalsIgnoreCase(type) || "change".equalsIgnoreCase(type)) return "rise";
        if ("marketcap".equalsIgnoreCase(type) || "cap".equalsIgnoreCase(type)) return "marketcap";
        if ("ai".equalsIgnoreCase(type) || "recommend".equalsIgnoreCase(type)) return "ai";
        if ("amount".equalsIgnoreCase(type)) return "amount";
        return "volume";
    }

    private static List<JsonNode> sortRowsByType(List<JsonNode> rows, String type) {
        if ("rise".equalsIgnoreCase(type)) {
            rows.sort(Comparator.comparingDouble((JsonNode r) -> r.path("prdy_ctrt").asDouble(0.0)).reversed());
        } else if ("marketcap".equalsIgnoreCase(type)) {
            rows.sort(Comparator.comparingLong((JsonNode r) -> marketCapOf(r)).reversed());
        } else if ("ai".equalsIgnoreCase(type)) {
            rows.sort(Comparator.comparingInt((JsonNode r) -> aiScoreOf(r)).reversed());
        }
        return rows;
    }

    private static List<TopStockDto> sortDtosByType(List<TopStockDto> rows, String type) {
        List<TopStockDto> copy = new ArrayList<>(rows);
        if ("rise".equalsIgnoreCase(type)) {
            copy.sort(Comparator.comparingDouble(TopStockDto::changeRate).reversed());
        } else if ("ai".equalsIgnoreCase(type)) {
            copy.sort(Comparator.comparingInt(TopStockDto::finalScore).reversed());
        } else if ("marketcap".equalsIgnoreCase(type)) {
            // dto에는 시총 필드가 없어 백엔드 row 단계 정렬 결과를 유지
        }
        return copy;
    }

    private static TopStockDto mapKisRankRow(JsonNode row, String type) {
        int rank = parseRank(row);
        String code = row.path("mksc_shrn_iscd").asText("").trim();
        String name = row.path("hts_kor_isnm").asText("").trim();
        long price = numLong(row, "stck_prpr");
        double chg = row.path("prdy_ctrt").asDouble(0);
        long vol = numLong(row, "acml_vol");
        int ai = "ai".equalsIgnoreCase(type) ? aiScoreOf(row) : 0;
        int fin = ai;
        return new TopStockDto(
                rank,
                code,
                name.isEmpty() ? code : name,
                price,
                chg,
                vol,
                0,
                ai,
                fin);
    }

    private static long marketCapOf(JsonNode row) {
        long shares = numLong(row, "lstn_stcn");
        long price = numLong(row, "stck_prpr");
        if (shares <= 0 || price <= 0) return 0L;
        return shares * price;
    }

    private static int aiScoreOf(JsonNode row) {
        double chg = row.path("prdy_ctrt").asDouble(0.0);
        long vol = numLong(row, "acml_vol");
        double volScore = Math.min(40.0, Math.log10(Math.max(1L, vol)) * 7.5);
        double momentum = Math.max(0.0, Math.min(60.0, chg * 6.0));
        return (int) Math.round(Math.max(0.0, Math.min(100.0, momentum + volScore)));
    }

    private static int parseRank(JsonNode row) {
        JsonNode r = row.get("data_rank");
        if (r == null || r.isNull()) {
            return 0;
        }
        if (r.isInt() || r.isLong()) {
            return r.asInt();
        }
        try {
            return Integer.parseInt(r.asText("").trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static long numLong(JsonNode row, String field) {
        JsonNode n = row.get(field);
        if (n == null || n.isNull()) {
            return 0L;
        }
        if (n.isNumber()) {
            return n.asLong();
        }
        String s = n.asText("").replace(",", "").trim();
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    public StockDetailDto getStockDetail(String stockCode, HttpServletRequest req) {
        String code = normalizeKisStockCode(stockCode);
        JsonNode priceRoot = kisBrokerService.inquirePrice(req, code);
        if (priceRoot.has("error")) {
            String msg = String.format(
                    "KIS 현재가 조회 실패: %s, httpStatus=%s, body=%s",
                    priceRoot.path("error").asText(),
                    priceRoot.path("httpStatus").asText(""),
                    priceRoot.path("responseBody").asText(""));
            log.warn("【KIS-DETAIL】 {}", msg);
            throw new IllegalStateException(msg);
        }
        if (!"0".equals(priceRoot.path("rt_cd").asText(""))) {
            String msg = String.format(
                    "KIS 현재가 비정상: rt_cd=%s, msg1=%s, msg2=%s",
                    priceRoot.path("rt_cd").asText(""),
                    priceRoot.path("msg1").asText(""),
                    priceRoot.path("msg2").asText(""));
            log.warn("【KIS-DETAIL】 {}", msg);
            throw new IllegalStateException(msg);
        }
        JsonNode out = priceRoot.path("output");
        if (out == null || out.isMissingNode() || out.isNull()) {
            throw new IllegalStateException("KIS 현재가 응답에 output 이 없습니다. stockCode=" + code);
        }
        String name = out.path("hts_kor_isnm").asText("").trim();
        long pr = numLong(out, "stck_prpr");
        double rate = out.path("prdy_ctrt").asDouble(0.0);
        long vol = numLong(out, "acml_vol");
        if (!StringUtils.hasText(name)) {
            name = code;
        }

        return new StockDetailDto(
                code,
                name,
                pr,
                rate,
                vol,
                List.of(),
                0,
                0,
                0,
                List.of());
    }

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HHmmss");

    /**
     * 차트 봉.t f: 1m 분봉(당일, 연속조회) · 1h 시(60분)봉 · 1d 일봉 · 1w 주봉 · 1mo 월봉
     */
    public StockCandlesDto getStockCandles(String stockCode, String tf, HttpServletRequest req) {
        String code = normalizeKisStockCode(stockCode);
        String raw = tf == null || tf.isBlank() ? "1d" : tf.trim();
        if ("1M".equals(raw)) {
            return new StockCandlesDto("1mo", fetchRangePeriodCandles(code, "M", req));
        }
        String t = raw.toLowerCase(Locale.ROOT);
        return switch (t) {
            case "1m", "m1" -> new StockCandlesDto("1m", fetchIntradayMinutes(code, req));
            case "1h", "h1" -> new StockCandlesDto("1h", fetchIntradayHourly(code, req));
            case "1d", "d1" -> new StockCandlesDto("1d", fetchRangePeriodCandles(code, "D", req));
            case "1w", "w1" -> new StockCandlesDto("1w", fetchRangePeriodCandles(code, "W", req));
            case "1mo", "1mth", "mon" -> new StockCandlesDto("1mo", fetchRangePeriodCandles(code, "M", req));
            default -> throw new IllegalArgumentException("지원하지 않는 tf 입니다. (1m,1h,1d,1w,1mo, 월봉 1M): " + tf);
        };
    }

    private List<CandleDto> fetchRangePeriodCandles(String code, String periodCode, HttpServletRequest req) {
        LocalDate end = LocalDate.now(SEOUL);
        LocalDate start = end.minusYears(3);
        JsonNode root =
                kisBrokerService.inquireDailyItemchartprice(
                        req, code, YMD.format(start), YMD.format(end), periodCode);
        return parseDailyItemchartOutput(root, periodCode);
    }

    private List<CandleDto> parseDailyItemchartOutput(JsonNode root, String periodCode) {
        if (root.has("error") || !"0".equals(root.path("rt_cd").asText(""))) {
            log.debug(
                    "【KIS-CANDLES】 itemchart 실패: error={}, rt_cd={}, period={}",
                    root.path("error").asText(""),
                    root.path("rt_cd").asText(""),
                    periodCode);
            return List.of();
        }
        JsonNode arr = root.path("output2");
        if (!arr.isArray()) {
            return List.of();
        }
        List<CandleDto> out = new ArrayList<>();
        for (JsonNode row : arr) {
            String ymd = row.path("stck_bsop_date").asText("").trim();
            if (ymd.length() != 8) {
                continue;
            }
            String time = toIsoDateYmd(ymd);
            long o = numLong(row, "stck_oprc");
            long h = numLong(row, "stck_hgpr");
            long l = numLong(row, "stck_lwpr");
            long c = numLong(row, "stck_clpr");
            if (c == 0L) {
                c = numLong(row, "stck_prpr");
            }
            long v = numLong(row, "acml_vol");
            if (v == 0L) {
                v = numLong(row, "cntg_vol");
            }
            out.add(new CandleDto(time, o, h, l, c, v));
        }
        return Collections.unmodifiableList(out);
    }

    private List<CandleDto> fetchIntradayMinutes(String code, HttpServletRequest req) {
        TreeMap<String, CandleDto> byKey = new TreeMap<>();
        ZonedDateTime seoul = ZonedDateTime.now(SEOUL);
        LocalTime nowT = seoul.toLocalTime();
        LocalTime open = LocalTime.of(9, 0);
        LocalTime close = LocalTime.of(15, 30);
        LocalTime ref = nowT;
        if (ref.isAfter(close)) {
            ref = close;
        }
        if (ref.isBefore(open)) {
            ref = open;
        }
        String hour = ref.format(HMS);
        for (int i = 0; i < 6; i++) {
            sleepQuietly(120);
            JsonNode root = kisBrokerService.inquireTimeItemchartprice(req, code, hour, "", "Y");
            if (root.has("error") || !"0".equals(root.path("rt_cd").asText(""))) {
                break;
            }
            List<CandleDto> batch = parseTimeItemchartOutput(root);
            if (batch.isEmpty()) {
                break;
            }
            for (CandleDto c : batch) {
                byKey.putIfAbsent(c.time(), c);
            }
            LocalDateTime minDt = null;
            for (CandleDto c : batch) {
                LocalDateTime d = parseCandleTime(c.time());
                if (d != null && (minDt == null || d.isBefore(minDt))) {
                    minDt = d;
                }
            }
            if (minDt == null) {
                break;
            }
            LocalDateTime next = minDt.minusMinutes(1);
            if (next.toLocalTime().isBefore(LocalTime.of(8, 59))) {
                break;
            }
            hour = next.format(HMS);
        }
        return new ArrayList<>(byKey.values());
    }

    private static void sleepQuietly(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private List<CandleDto> fetchIntradayHourly(String code, HttpServletRequest req) {
        ZonedDateTime seoul = ZonedDateTime.now(SEOUL);
        LocalTime nowT = seoul.toLocalTime();
        LocalTime open = LocalTime.of(9, 0);
        LocalTime close = LocalTime.of(15, 30);
        LocalTime ref = nowT;
        if (ref.isAfter(close)) {
            ref = close;
        }
        if (ref.isBefore(open)) {
            ref = open;
        }
        String anchor = ref.format(HMS);
        JsonNode root = kisBrokerService.inquireTimeItemchartprice(req, code, anchor, "3600", "Y");
        if (!root.has("error") && "0".equals(root.path("rt_cd").asText(""))) {
            List<CandleDto> fromApi = parseTimeItemchartOutput(root);
            if (!fromApi.isEmpty()) {
                return fromApi;
            }
        }
        return resampleToHour(fetchIntradayMinutes(code, req));
    }

    private static List<CandleDto> resampleToHour(List<CandleDto> minuteBars) {
        if (minuteBars.isEmpty()) {
            return List.of();
        }
        minuteBars = new ArrayList<>(minuteBars);
        minuteBars.sort(Comparator.comparing(CandleDto::time));
        Map<String, List<CandleDto>> byHour = new LinkedHashMap<>();
        for (CandleDto c : minuteBars) {
            LocalDateTime dt = parseCandleTime(c.time());
            if (dt == null) {
                continue;
            }
            LocalDateTime hourStart = dt.withMinute(0).withSecond(0).withNano(0);
            byHour.computeIfAbsent(hourStart.toString(), k -> new ArrayList<>()).add(c);
        }
        List<CandleDto> out = new ArrayList<>();
        for (Map.Entry<String, List<CandleDto>> e : byHour.entrySet()) {
            List<CandleDto> g = e.getValue();
            if (g.isEmpty()) {
                continue;
            }
            long o = g.get(0).open();
            long h = g.stream().mapToLong(CandleDto::high).max().orElse(0L);
            long l = g.stream().mapToLong(CandleDto::low).min().orElse(0L);
            long cl = g.get(g.size() - 1).close();
            long v = g.stream().mapToLong(CandleDto::volume).sum();
            out.add(new CandleDto(e.getKey(), o, h, l, cl, v));
        }
        return out;
    }

    private List<CandleDto> parseTimeItemchartOutput(JsonNode root) {
        if (root.has("error") || !"0".equals(root.path("rt_cd").asText(""))) {
            return List.of();
        }
        JsonNode arr = root.path("output2");
        if (!arr.isArray()) {
            return List.of();
        }
        List<CandleDto> out = new ArrayList<>();
        for (JsonNode row : arr) {
            String ymd = row.path("stck_bsop_date").asText("").trim();
            String hms = row.path("stck_cntg_hour").asText("").replaceAll("\\D", "").trim();
            if (ymd.length() != 8 || hms.length() < 4) {
                continue;
            }
            while (hms.length() < 6) {
                hms = "0" + hms;
            }
            if (hms.length() > 6) {
                hms = hms.substring(hms.length() - 6);
            }
            String time = toIsoDateYmd(ymd) + "T" + hms.substring(0, 2) + ":" + hms.substring(2, 4) + ":"
                    + hms.substring(4, 6);
            long o = numLong(row, "stck_oprc");
            long h = numLong(row, "stck_hgpr");
            long l = numLong(row, "stck_lwpr");
            long c = numLong(row, "stck_prpr");
            long v = numLong(row, "cntg_vol");
            if (c == 0L) {
                c = numLong(row, "stck_clpr");
            }
            out.add(new CandleDto(time, o, h, l, c, v));
        }
        return out;
    }

    private static LocalDateTime parseCandleTime(String time) {
        if (!StringUtils.hasText(time)) {
            return null;
        }
        try {
            if (time.contains("T")) {
                return LocalDateTime.parse(time);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 숫자만 남기고 6자리로 맞춤 (KIS 국내 일반·ETF) */
    private static String normalizeKisStockCode(String raw) {
        if (raw == null) {
            return "";
        }
        String d = raw.replaceAll("\\D", "");
        if (d.isEmpty()) {
            return raw.trim();
        }
        if (d.length() > 6) {
            d = d.substring(0, 6);
        }
        return String.format("%6s", d).replace(' ', '0');
    }

    private static String toIsoDateYmd(String ymd) {
        if (ymd == null || ymd.length() != 8) {
            return ymd == null ? "" : ymd;
        }
        return ymd.substring(0, 4) + "-" + ymd.substring(4, 6) + "-" + ymd.substring(6, 8);
    }
}
