package com.noono0.stock.broker.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.broker.service.KisOrderRecordService;
import com.noono0.stock.broker.util.KisCcnlOutputParser;
import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.integration.kis.service.KisBrokerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/broker/kis")
@RequiredArgsConstructor
public class KisBrokerController {
    private final KisBrokerService kisBrokerService;
    private final KisOrderRecordService kisOrderRecordService;
    private final ObjectMapper objectMapper;

    /** 토큰 발급만 시험 — 로그에 【KIS-TOKEN】 이 찍히는지 확인용 (우리 서버 URL이 아님, 아래 참고) */
    @GetMapping("/oauth/ping")
    public ApiResponse<?> oauthPing(HttpServletRequest req) {
        return ApiResponse.ok(kisBrokerService.pingOAuthToken(req));
    }

    @GetMapping("/price/{iscd}")
    public ApiResponse<?> price(HttpServletRequest req, @PathVariable("iscd") String iscd) {
        return ApiResponse.ok(kisBrokerService.inquirePrice(req, iscd));
    }

    @GetMapping("/balance")
    public ApiResponse<?> balance(HttpServletRequest req) {
        return ApiResponse.ok(kisBrokerService.inquireBalance(req));
    }

    /**
     * 시장가 매수 후 {@link com.noono0.stock.broker.domain.BrokerOrderAttempt}에 기록.
     *
     * @param reason 선택 — 주문 사유(휴리스틱/전략 요약·JSON)
     */
    @PostMapping("/order/buy")
    public ApiResponse<?> buy(
            HttpServletRequest req,
            @RequestParam(name = "pdno") String pdno,
            @RequestParam(name = "qty") int qty,
            @RequestParam(name = "reason", required = false) String reason) {
        var o = kisOrderRecordService.placeBuy(req, pdno, qty, reason);
        return ApiResponse.ok(
                Map.of(
                        "attemptId", o.attemptId(),
                        "clientOrderKey", o.clientOrderKey(),
                        "kis", o.kis()));
    }

    /**
     * 시장가 매도
     *
     * @param reason 선택
     */
    @PostMapping("/order/sell")
    public ApiResponse<?> sell(
            HttpServletRequest req,
            @RequestParam(name = "pdno") String pdno,
            @RequestParam(name = "qty") int qty,
            @RequestParam(name = "reason", required = false) String reason) {
        var o = kisOrderRecordService.placeSell(req, pdno, qty, reason);
        return ApiResponse.ok(
                Map.of(
                        "attemptId", o.attemptId(),
                        "clientOrderKey", o.clientOrderKey(),
                        "kis", o.kis()));
    }

    @PostMapping("/order/cancel")
    public ApiResponse<?> cancel(HttpServletRequest req, @RequestBody JsonNode body) {
        return ApiResponse.ok(kisBrokerService.orderCancel(req, body));
    }

    /** KIS 일별체결 JSON 원문 */
    @GetMapping("/executions/daily")
    public ApiResponse<?> executions(HttpServletRequest req) {
        return ApiResponse.ok(kisBrokerService.inquireDailyCcnl(req));
    }

    /** 일별체결: 원문 + 파싱된 행(시간/종목/매수매도/가격) */
    @GetMapping("/executions/daily/view")
    public ApiResponse<?> executionsView(HttpServletRequest req) {
        log.info("【BROKER-ORDER】 ··· 일별 체결 뷰 API 호출 (raw+rows 파싱)");
        JsonNode raw = kisBrokerService.inquireDailyCcnl(req);
        List<?> rows = KisCcnlOutputParser.parseRows(raw, objectMapper);
        log.info("【BROKER-ORDER】   ↳ 일별 체결 파싱 행 수 = {} 건", rows.size());
        return ApiResponse.ok(Map.of("raw", raw, "rows", rows));
    }

    /** 앱이 기록한 주문(매수/매도 시도) 최근 N건 */
    @GetMapping("/orders/recent")
    public ApiResponse<?> orderAttempts(@RequestParam(name = "limit") int limit) {
        return ApiResponse.ok(kisOrderRecordService.recent(limit));
    }
}
