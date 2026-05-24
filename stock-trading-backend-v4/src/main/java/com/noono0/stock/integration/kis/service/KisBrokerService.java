package com.noono0.stock.integration.kis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.noono0.stock.integration.kis.config.KisProperties;
import com.noono0.stock.integration.kis.credential.service.UserKisCredentialService;
import com.noono0.stock.integration.kis.oauth.KisOAuthClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.noono0.stock.integration.kis.KisEffectiveMode.from;
import static com.noono0.stock.integration.kis.KisEffectiveMode.fromConfigOnly;

/**
 * 한국투자 REST (현재가·잔고·주문·정정취소·체결조회).<br>
 * 공식 TR_ID·파라미터는 한국투자 개발자 센터 최신 문서와 대조해 주세요.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KisBrokerService {

    /** 국내 거래량순위 (순위분석 v1_국내주식-047). 공식 샘플은 FHPST01710000(세 번째 글자 P) 사용. */
    public static final String TR_VOLUME_RANK = "FHPST01710000";

    private final KisOAuthClient oauth;
    private final KisProperties props;
    private final UserKisCredentialService userCredentialService;
    private final ObjectMapper om;
    private final RestClient restClient;

    /**
     * 한국투자 OAuth 토큰 발급만 수행해 성공 여부를 확인한다.
     * <p>실제 HTTP는 <b>한국투자 서버</b>로 나가며, 이 Spring 앱에는 {@code /oauth2/tokenP} 라우트가 없다.
     * 로그 {@code 【KIS-TOKEN】} 는 이 메서드 또는 다른 KIS API 호출 시 출력된다.
     */
    public JsonNode pingOAuthToken(HttpServletRequest req) {
        String mode = from(props, req);
        UserKisCredentialService.ResolvedCredential c = resolveCredential(mode, req);
        oauth.accessToken(mode, c.appKey(), c.appSecret());
        ObjectNode o = om.createObjectNode();
        o.put("ok", true);
        o.put("mode", mode);
        o.put("credentialScope", c.userScoped() ? "user" : "server");
        o.put(
                "kisTokenUrl",
                "real".equalsIgnoreCase(mode)
                        ? "https://openapi.koreainvestment.com:9443/oauth2/tokenP"
                        : "https://openapivts.koreainvestment.com:29443/oauth2/tokenP");
        o.put("note", "access_token 은 JSON에 포함하지 않음. 백엔드 로그의 【KIS-TOKEN】 확인.");
        return o;
    }

    /** 국내 주식 현재가 */
    public JsonNode inquirePrice(HttpServletRequest req, String iscd) {
        String mode = from(props, req);
        UserKisCredentialService.ResolvedCredential c = resolveCredential(mode, req);
        return get(mode, "/uapi/domestic-stock/v1/quotations/inquire-price", Map.of(
                "FID_COND_MRKT_DIV_CODE", "J",
                "FID_INPUT_ISCD", iscd
        ), "FHKST01010100", c);
    }

    /** HTTP 요청 없이 {@code app.kis.mode} 기준 현재가 (백테스트·배치용) */
    public JsonNode inquirePriceForConfig(String iscd) {
        String mode = fromConfigOnly(props);
        UserKisCredentialService.ResolvedCredential c = resolveCredential(mode, null);
        return get(mode, "/uapi/domestic-stock/v1/quotations/inquire-price", Map.of(
                "FID_COND_MRKT_DIV_CODE", "J",
                "FID_INPUT_ISCD", iscd
        ), "FHKST01010100", c);
    }

    /** 국내 주식 일봉(최근 30거래일, 일간). {@link #inquirePrice(HttpServletRequest, String)} 와 동일 {@code mode}. */
    public JsonNode inquireDailyPrice(HttpServletRequest req, String iscd) {
        String mode = from(props, req);
        UserKisCredentialService.ResolvedCredential c = resolveCredential(mode, req);
        return get(
                mode,
                "/uapi/domestic-stock/v1/quotations/inquire-daily-price",
                Map.of(
                        "FID_COND_MRKT_DIV_CODE", "J",
                        "FID_INPUT_ISCD", iscd,
                        "FID_PERIOD_DIV_CODE", "D",
                        "FID_ORG_ADJ_PRC", "1"),
                "FHKST01010400",
                c);
    }

    /** 국내주식기간별시세(일/주/월) TR — 한 번에 최대 100건. */
    public static final String TR_INQUIRE_DAILY_ITEMCHART = "FHKST03010100";

    /** 당일 분/시봉 TR — 한 번에 최대 30건(당일 위주). */
    public static final String TR_INQUIRE_TIME_ITEMCHART = "FHKST03010200";

    /**
     * 국내주식기간별시세(일/주/월) — FID_PERIOD_DIV_CODE: D, W, M (공식 API 문서 기준)
     */
    public JsonNode inquireDailyItemchartprice(
            HttpServletRequest req, String iscd, String startYmd, String endYmd, String periodDivCode) {
        String mode = from(props, req);
        UserKisCredentialService.ResolvedCredential c = resolveCredential(mode, req);
        return get(
                mode,
                "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice",
                Map.of(
                        "FID_COND_MRKT_DIV_CODE", "J",
                        "FID_INPUT_ISCD", iscd,
                        "FID_INPUT_DATE_1", startYmd,
                        "FID_INPUT_DATE_2", endYmd,
                        "FID_PERIOD_DIV_CODE", periodDivCode,
                        "FID_ORG_ADJ_PRC", "1"),
                TR_INQUIRE_DAILY_ITEMCHART,
                c);
    }

    /**
     * 당일 분봉·N분/시(초) 단위. {@code FID_ETC_CLS_CODE} 는 공백(1분) 또는 초 단위(예: 3600=60분) 등 문서에 따름.
     */
    public JsonNode inquireTimeItemchartprice(
            HttpServletRequest req,
            String iscd,
            String fidInputHour1,
            String fidEtcClsCode,
            String fidPwDataIncuYn) {
        String mode = from(props, req);
        UserKisCredentialService.ResolvedCredential c = resolveCredential(mode, req);
        return get(
                mode,
                "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice",
                Map.of(
                        "FID_COND_MRKT_DIV_CODE", "J",
                        "FID_INPUT_ISCD", iscd,
                        "FID_INPUT_HOUR_1", fidInputHour1,
                        "FID_PW_DATA_INCU_YN", fidPwDataIncuYn,
                        "FID_ETC_CLS_CODE", fidEtcClsCode == null ? "" : fidEtcClsCode),
                TR_INQUIRE_TIME_ITEMCHART,
                c);
    }

    /** 주식 잔고 */
    public JsonNode inquireBalance(HttpServletRequest req) {
        String mode = from(props, req);
        UserKisCredentialService.ResolvedCredential c = resolveCredential(mode, req);
        String tr = "real".equalsIgnoreCase(mode) ? "TTTC8434R" : "VTTC8434R";
        if (!StringUtils.hasText(c.accountNo())) {
            return om.createObjectNode().put("error", "KIS 계좌번호 미설정(설정 페이지에서 계좌번호를 저장하세요).");
        }
        return get(
                mode,
                "/uapi/domestic-stock/v1/trading/inquire-balance",
                Map.of(
                        "CANO", c.accountNo(),
                        "ACNT_PRDT_CD", nz(c.productCode(), "01"),
                        "AFHR_FLPR_YN", "N",
                        "OFL_YN", "",
                        "INQR_DVSN", "02",
                        "UNPR_DVSN", "01",
                        "FUND_STTL_ICLD_YN", "N",
                        "FNCG_AMT_AUTO_RDPT_YN", "N",
                        "PRCS_DVSN", "01"
                ),
                tr,
                c);
    }

    /** 현금 매수 (시장가 예시) */
    public JsonNode orderBuyMarket(HttpServletRequest req, String pdno, int qty) {
        String mode = from(props, req);
        UserKisCredentialService.ResolvedCredential c = resolveCredential(mode, req);
        String tr = "real".equalsIgnoreCase(mode) ? "TTTC0802U" : "VTTC0802U";
        log.warn("【KIS-ORDER】 ★ 시장가 매수 주문 API 호출 ★ TR={} mode={} pdno={} qty={}", tr, mode, pdno, qty);
        ObjectNode body = om.createObjectNode();
        body.put("CANO", c.accountNo());
        body.put("ACNT_PRDT_CD", nz(c.productCode(), "01"));
        body.put("PDNO", pdno);
        body.put("ORD_DVSN", "01");
        body.put("ORD_QTY", String.valueOf(qty));
        body.put("ORD_UNPR", "0");
        return postOrderWithJson(mode, tr, body, c);
    }

    /** 현금 매도 (시장가) — TR: 실전 TTTC0801U / 모의 VTTC0801U */
    public JsonNode orderSellMarket(HttpServletRequest req, String pdno, int qty) {
        String mode = from(props, req);
        UserKisCredentialService.ResolvedCredential c = resolveCredential(mode, req);
        String tr = "real".equalsIgnoreCase(mode) ? "TTTC0801U" : "VTTC0801U";
        log.warn("【KIS-ORDER】 ★ 시장가 매도 주문 API 호출 ★ TR={} mode={} pdno={} qty={}", tr, mode, pdno, qty);
        ObjectNode body = om.createObjectNode();
        body.put("CANO", c.accountNo());
        body.put("ACNT_PRDT_CD", nz(c.productCode(), "01"));
        body.put("PDNO", pdno);
        body.put("ORD_DVSN", "01");
        body.put("ORD_QTY", String.valueOf(qty));
        body.put("ORD_UNPR", "0");
        return postOrderWithJson(mode, tr, body, c);
    }

    /** 주문 취소 (orgOrdNo 등 필요 — 샘플은 전달 JSON 그대로 전송) */
    public JsonNode orderCancel(HttpServletRequest req, JsonNode cancelBody) {
        String mode = from(props, req);
        UserKisCredentialService.ResolvedCredential c = resolveCredential(mode, req);
        String tr = "real".equalsIgnoreCase(mode) ? "TTTC0803U" : "VTTC0803U";
        if (cancelBody instanceof ObjectNode on) {
            return postOrderWithJson(mode, tr, on, c);
        }
        return om.createObjectNode().put("error", "JSON Object 필요");
    }

    /** 일별 체결 내역 */
    public JsonNode inquireDailyCcnl(HttpServletRequest req) {
        String mode = from(props, req);
        UserKisCredentialService.ResolvedCredential c = resolveCredential(mode, req);
        String tr = "real".equalsIgnoreCase(mode) ? "TTTC8001R" : "VTTC8001R";
        log.warn(
                "【KIS-ORDER】 일별 체결 조회 TR={} mode={} (최근 7일~당일)",
                tr, mode);
        return get(
                mode,
                "/uapi/domestic-stock/v1/trading/inquire-daily-ccld",
                Map.of(
                        "CANO", nz(c.accountNo(), ""),
                        "ACNT_PRDT_CD", nz(c.productCode(), "01"),
                        "INQR_STRT_DT", java.time.LocalDate.now().minusDays(7).toString().replace("-", ""),
                        "INQR_END_DT", java.time.LocalDate.now().toString().replace("-", "")
                ),
                tr,
                c);
    }

    /**
     * 거래량/거래대금 등 순위 1페이지. 연속 조회는 응답 헤더 {@code tr_cont} 가 {@code M} 이면 다음 요청에 {@code tr_cont=N}.
     *
     * @param fidBlngClsCode 0=거래량, 3=거래금액 (공식 문서 기준)
     * @param requestTrCont 첫 페이지는 {@code null} 또는 빈 문자열, 이후 {@code N}
     */
    public VolumeRankPage inquireVolumeRankPage(
            HttpServletRequest req, String fidBlngClsCode, String requestTrCont) {
        String mode = from(props, req);
        UserKisCredentialService.ResolvedCredential c = resolveCredential(mode, req);
        Map<String, String> q = new LinkedHashMap<>();
        q.put("FID_COND_MRKT_DIV_CODE", "J");
        q.put("FID_COND_SCR_DIV_CODE", "20171");
        q.put("FID_INPUT_ISCD", "0000");
        q.put("FID_DIV_CLS_CODE", "0");
        q.put("FID_BLNG_CLS_CODE", fidBlngClsCode);
        q.put("FID_TRGT_CLS_CODE", "111111111");
        q.put("FID_TRGT_EXLS_CLS_CODE", "0000000000");
        q.put("FID_INPUT_PRICE_1", "0");
        q.put("FID_INPUT_PRICE_2", "1000000");
        q.put("FID_VOL_CNT", "100000");
        q.put("FID_INPUT_DATE_1", "");
        return getVolumeRank(mode, q, requestTrCont, c);
    }

    public record VolumeRankPage(JsonNode body, String responseTrCont) {}

    private VolumeRankPage getVolumeRank(
            String mode,
            Map<String, String> query,
            String requestTrCont,
            UserKisCredentialService.ResolvedCredential c) {
        try {
            String token = oauth.accessToken(mode, c.appKey(), c.appSecret()); // 토큰 로그는 KisOAuthClient
            String base = oauth.restBase(mode);
            UriComponentsBuilder ub =
                    UriComponentsBuilder.fromHttpUrl(base + "/uapi/domestic-stock/v1/quotations/volume-rank");
            query.forEach(ub::queryParam);
            URI uri = ub.build(true).toUri();
            ResponseEntity<String> entity =
                    restClient
                            .get()
                            .uri(uri)
                            .headers(
                                    h -> {
                                        headers(h, token, TR_VOLUME_RANK, c);
                                        if (StringUtils.hasText(requestTrCont)) {
                                            h.set("tr_cont", requestTrCont);
                                        }
                                    })
                            .retrieve()
                            .toEntity(String.class);
            JsonNode body = om.readTree(entity.getBody());
            String trCont = entity.getHeaders().getFirst("tr_cont");
            return new VolumeRankPage(body, trCont);
        } catch (RestClientResponseException ex) {
            return new VolumeRankPage(kisHttpError("거래량순위(/quotations/volume-rank)", ex), null);
        } catch (Exception exception) {
            log.error("【KIS-REST】 ★ 거래량순위 ★ {}", exception.getMessage(), exception);
            return new VolumeRankPage(error(exception.getMessage()), null);
        }
    }

    private JsonNode get(
            String mode,
            String path,
            Map<String, String> query,
            String trId,
            UserKisCredentialService.ResolvedCredential c) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                String token = oauth.accessToken(mode, c.appKey(), c.appSecret());
                String base = oauth.restBase(mode);
                UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(base + path);
                query.forEach(ub::queryParam);
                URI uri = ub.build(true).toUri();
                String json =
                        restClient
                                .get()
                                .uri(uri)
                                .headers(h -> headers(h, token, trId, c))
                                .retrieve()
                                .body(String.class);
                return om.readTree(json);
            } catch (RestClientResponseException ex) {
                String body = responseBodyUtf8(ex);
                // KIS는 초당 호출 초과 시 HTTP 500 + EGW00201을 반환하므로 짧게 백오프 후 재시도한다.
                if (isTooManyRequests(body) && attempt < 2) {
                    long waitMs = (attempt + 1L) * 200L;
                    log.warn(
                            "【KIS-REST】 RATE LIMIT 감지(GET {}) attempt={}/3, {}ms 후 재시도",
                            path,
                            attempt + 1,
                            waitMs);
                    sleepQuietly(waitMs);
                    continue;
                }
                return kisHttpError("GET " + path, ex, body);
            } catch (RestClientException | java.io.IOException exception) {
                log.error("【KIS-REST】 ★ GET {} ★ {}", path, exception.getMessage(), exception);
                return error(exception.getMessage());
            }
        }
        return error("KIS GET 재시도 실패: " + path);
    }

    private JsonNode postOrderWithJson(
            String mode, String trId, ObjectNode body, UserKisCredentialService.ResolvedCredential c) {
        try {
            if (!StringUtils.hasText(c.accountNo())) {
                return om.createObjectNode().put("error", "계좌번호 미설정");
            }
            String base = oauth.restBase(mode);
            String jsonBody = om.writeValueAsString(body);
            String hashJson = hashKey(mode, jsonBody, c);
            JsonNode hashNode = om.readTree(hashJson);
            if (hashNode.has("HASH")) {
                body.put("HASH", hashNode.get("HASH").asText());
            }
            String token = oauth.accessToken(mode, c.appKey(), c.appSecret());
            String res =
                    restClient
                            .post()
                            .uri(base + "/uapi/domestic-stock/v1/trading/order-cash")
                            .headers(h -> {
                                headers(h, token, trId, c);
                                h.setContentType(MediaType.APPLICATION_JSON);
                            })
                            .body(om.writeValueAsString(body))
                            .retrieve()
                            .body(String.class);
            return om.readTree(res);
        } catch (Exception exception) {
            return error(exception.getMessage());
        }
    }

    private String hashKey(String mode, String bodyJson, UserKisCredentialService.ResolvedCredential c)
            throws Exception {
        String token = oauth.accessToken(mode, c.appKey(), c.appSecret());
        String base = oauth.restBase(mode);
        return restClient
                .post()
                .uri(base + "/uapi/hashkey")
                .headers(h -> {
                    h.setBearerAuth(token);
                    h.set("appkey", c.appKey());
                    h.set("appsecret", c.appSecret());
                    h.set("tr_id", "HASH");
                    h.set("custtype", "P");
                    h.setContentType(MediaType.APPLICATION_JSON);
                })
                .body(bodyJson)
                .retrieve()
                .body(String.class);
    }

    private void headers(
            HttpHeaders h, String token, String trId, UserKisCredentialService.ResolvedCredential c) {
        h.setBearerAuth(token);
        h.set("appkey", c.appKey());
        h.set("appsecret", c.appSecret());
        h.set("tr_id", trId);
        h.set("custtype", "P");
    }

    private UserKisCredentialService.ResolvedCredential resolveCredential(String mode, HttpServletRequest req) {
        UserKisCredentialService.ResolvedCredential c = userCredentialService.resolveForRequest(mode, req);
        if (!StringUtils.hasText(c.appKey()) || !StringUtils.hasText(c.appSecret())) {
            throw new IllegalStateException("KIS appKey/appSecret 이 비어 있습니다. 설정에서 연동 정보를 확인하세요.");
        }
        return c;
    }

    private static String nz(String s, String d) {
        return StringUtils.hasText(s) ? s : d;
    }

    /** HTTP 4xx/5xx 시 응답 본문까지 로그·JSON에 담는다. */
    private JsonNode kisHttpError(String phase, RestClientResponseException ex) {
        return kisHttpError(phase, ex, responseBodyUtf8(ex));
    }

    private JsonNode kisHttpError(String phase, RestClientResponseException ex, String body) {
        if (isTooManyRequests(body)) {
            log.warn(
                    "【KIS-REST】 ★ {} ★ HTTP {} {} 응답본문={}",
                    phase,
                    ex.getStatusCode().value(),
                    ex.getStatusText(),
                    body);
        } else {
        log.error(
                "【KIS-REST】 ★ {} ★ HTTP {} {} 응답본문={}",
                phase,
                ex.getStatusCode().value(),
                ex.getStatusText(),
                body);
        }
        ObjectNode o = om.createObjectNode();
        o.put("error", "HTTP " + ex.getStatusCode().value() + " " + phase + ": " + body);
        o.put("httpStatus", ex.getStatusCode().value());
        o.put("phase", phase);
        o.put("responseBody", body);
        return o;
    }

    private static String responseBodyUtf8(RestClientResponseException ex) {
        try {
            return ex.getResponseBodyAsString(StandardCharsets.UTF_8);
        } catch (Exception exception) {
            return "(응답 본문 읽기 실패) " + exception.getMessage();
        }
    }

    private static boolean isTooManyRequests(String body) {
        return body != null && body.contains("EGW00201");
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private JsonNode error(String m) {
        return om.createObjectNode().put("error", m);
    }
}
