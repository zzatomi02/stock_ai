package com.noono0.stock.broker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.noono0.stock.broker.domain.BrokerOrderAttempt;
import com.noono0.stock.broker.mapper.BrokerOrderAttemptMapper;
import com.noono0.stock.integration.kis.config.KisProperties;
import com.noono0.stock.integration.kis.service.KisBrokerService;
import com.noono0.stock.risk.RiskGate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.noono0.stock.integration.kis.KisEffectiveMode.from;

/**
 * KIS 시장가 주문 후 {@link BrokerOrderAttempt}에 원문·사유를 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KisOrderRecordService {
    private final KisBrokerService kisBrokerService;
    private final BrokerOrderAttemptMapper brokerOrderAttemptMapper;
    private final KisProperties kisProperties;
    private final RiskGate riskGate;

    public record OrderWithAttempt(JsonNode kis, Long attemptId, String clientOrderKey) {}

    public OrderWithAttempt placeBuy(HttpServletRequest req, String pdno, int qty, String reasoning) {
        return placeBuy(req, pdno, qty, reasoning, null);
    }

    public OrderWithAttempt placeBuy(
            HttpServletRequest req, String pdno, int qty, String reasoning, Long signalId) {
        return placeBuy(req, pdno, qty, reasoning, signalId, null);
    }

    public OrderWithAttempt placeBuy(
            HttpServletRequest req,
            String pdno,
            int qty,
            String reasoning,
            Long signalId,
            String strategyType) {
        String mode = from(kisProperties, req);
        validateBeforeOrder(req, pdno, "BUY", signalId, mode, qty, strategyType);
        log.info(
                "【BROKER-ORDER】 ═══ 시장가 매수 진행 ═══ mode={} pdno={} qty={} 사유={}",
                mode,
                pdno,
                qty,
                StringUtils.hasText(reasoning) ? "있음" : "없음");
        JsonNode r = kisBrokerService.orderBuyMarket(req, pdno, qty);
        log.info("【BROKER-ORDER】   ↳ KIS 응답 rt_cd={} (매수)", rtCd(r));
        OrderWithAttempt out = save(req, pdno, "BUY", qty, reasoning, r, signalId);
        afterOrderAccepted(req, pdno, "BUY", qty, strategyType);
        return out;
    }

    public OrderWithAttempt placeSell(HttpServletRequest req, String pdno, int qty, String reasoning) {
        return placeSell(req, pdno, qty, reasoning, null);
    }

    public OrderWithAttempt placeSell(
            HttpServletRequest req, String pdno, int qty, String reasoning, Long signalId) {
        return placeSell(req, pdno, qty, reasoning, signalId, null);
    }

    public OrderWithAttempt placeSell(
            HttpServletRequest req,
            String pdno,
            int qty,
            String reasoning,
            Long signalId,
            String strategyType) {
        String mode = from(kisProperties, req);
        validateBeforeOrder(req, pdno, "SELL", signalId, mode, qty, strategyType);
        log.info(
                "【BROKER-ORDER】 ═══ 시장가 매도 진행 ═══ mode={} pdno={} qty={} 사유={}",
                mode,
                pdno,
                qty,
                StringUtils.hasText(reasoning) ? "있음" : "없음");
        JsonNode r = kisBrokerService.orderSellMarket(req, pdno, qty);
        log.info("【BROKER-ORDER】   ↳ KIS 응답 rt_cd={} (매도)", rtCd(r));
        OrderWithAttempt out = save(req, pdno, "SELL", qty, reasoning, r, signalId);
        afterOrderAccepted(req, pdno, "SELL", qty, strategyType);
        return out;
    }

    private void validateBeforeOrder(
            HttpServletRequest req,
            String pdno,
            String side,
            Long signalId,
            String mode,
            int qty,
            String strategyType) {
        String userId = req != null ? req.getHeader("X-User-Id") : null;
        long estAmount = (long) qty * 50_000L;
        var result =
                riskGate.validateOrder(userId, pdno, side, signalId, mode, estAmount, strategyType, qty);
        if (!result.passed()) {
            throw new com.noono0.stock.execution.OrderGatewayException(
                    result.message().isBlank() ? "주문 전 검증 실패" : result.message());
        }
    }

    private void afterOrderAccepted(
            HttpServletRequest req,
            String pdno,
            String side,
            int qty,
            String strategyType) {
        String userId = req != null ? req.getHeader("X-User-Id") : null;
        riskGate.onOrderAccepted(userId, pdno, side, strategyType, (long) qty * 50_000L, qty);
    }

    public List<BrokerOrderAttempt> recent(int limit) {
        int n = Math.min(200, Math.max(1, limit));
        var list = brokerOrderAttemptMapper.findRecent(n);
        log.info("【BROKER-ORDER】 주문 시도 이력 조회 ★ limit={} → {}건", n, list.size());
        return list;
    }

    private OrderWithAttempt save(
            HttpServletRequest req,
            String pdno,
            String side,
            int qty,
            String reasoning,
            JsonNode kis,
            Long signalId) {
        String mode = from(kisProperties, req);
        BrokerOrderAttempt b = new BrokerOrderAttempt();
        b.setClientOrderKey("U-" + UUID.randomUUID().toString().replace("-", ""));
        b.setStockCode(pdno);
        b.setSide(side);
        b.setQuantity(qty);
        b.setMode(mode);
        b.setRawResponse(kis != null ? kis.toString() : "");
        b.setReasoning(StringUtils.hasText(reasoning) ? reasoning.trim() : null);
        b.setSignalId(signalId);
        b.setReasonSnapshot(StringUtils.hasText(reasoning) ? reasoning.trim() : null);
        b.setCreatedAt(LocalDateTime.now());
        brokerOrderAttemptMapper.insert(b);
        log.info(
                "【BROKER-ORDER】 ★ DB 기록 완료 ★ id={} side={} mode={} key={} rt_cd={}",
                b.getId(),
                side,
                mode,
                b.getClientOrderKey(),
                rtCd(kis));
        return new OrderWithAttempt(kis, b.getId(), b.getClientOrderKey());
    }

    private static String rtCd(JsonNode k) {
        if (k == null || k.isNull()) return "(null)";
        if (k.hasNonNull("rt_cd")) return k.get("rt_cd").asText();
        JsonNode out = k.get("output");
        if (out != null && out.isObject() && out.hasNonNull("rt_cd")) {
            return out.get("rt_cd").asText();
        }
        return "(없음)";
    }
}
