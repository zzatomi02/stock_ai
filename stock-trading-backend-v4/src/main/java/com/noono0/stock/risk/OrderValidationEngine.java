package com.noono0.stock.risk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.execution.ExecutionPhase;
import com.noono0.stock.execution.OrderGateway;
import com.noono0.stock.market.service.MarketMoodService;
import com.noono0.stock.ops.trading.TradingControlService;
import com.noono0.stock.risk.domain.OrderValidationLog;
import com.noono0.stock.risk.dto.RiskCheckResult;
import com.noono0.stock.risk.repository.OrderValidationLogRepository;
import com.noono0.stock.risk.service.RiskManagementEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrderValidationEngine {
    private final OrderGateway orderGateway;
    private final TradingControlService tradingControlService;
    private final MarketMoodService marketMoodService;
    private final OrderValidationLogRepository logRepository;
    private final ObjectMapper objectMapper;
    private final RiskManagementEngine riskManagementEngine;

    public record ValidationResult(boolean passed, String failReason, Map<String, Object> checks) {}

    public ValidationResult validate(
            String userId,
            String stockCode,
            String side,
            Long signalId,
            Long orderId,
            String kisMode,
            long orderAmount) {
        return validate(userId, stockCode, side, signalId, orderId, kisMode, orderAmount, null, 0);
    }

    public ValidationResult validate(
            String userId,
            String stockCode,
            String side,
            Long signalId,
            Long orderId,
            String kisMode,
            long orderAmount,
            String strategyType,
            int quantity) {
        Map<String, Object> checks = new LinkedHashMap<>();
        boolean passed = true;
        StringBuilder fail = new StringBuilder();

        check(checks, "autoTradeEnabled", false, "자동매매 OFF(관찰·수동 기본)");
        check(checks, "emergencyStop", tradingControlService.isEmergencyStop(), "긴급정지 ON");
        if (tradingControlService.isEmergencyStop()) {
            passed = false;
            fail.append("긴급정지 활성. ");
        }

        ExecutionPhase phase = orderGateway.currentPhase();
        checks.put("executionPhase", phase.name());
        try {
            orderGateway.assertKisOrderAllowed(kisMode);
            check(checks, "executionPhaseAllowsOrder", true, null);
        } catch (Exception exception) {
            passed = false;
            check(checks, "executionPhaseAllowsOrder", false, exception.getMessage());
            fail.append(exception.getMessage()).append(" ");
        }

        check(
                checks,
                "realTradeAllowed",
                !"real".equalsIgnoreCase(kisMode)
                        || phase == ExecutionPhase.REAL_MANUAL
                        || phase == ExecutionPhase.REAL_AUTO,
                null);

        long amount = orderAmount > 0 ? orderAmount : estimateAmount(quantity);
        RiskManagementEngine.OrderRiskContext riskCtx =
                new RiskManagementEngine.OrderRiskContext(
                        userId, stockCode, side, strategyType, amount, quantity);
        RiskCheckResult riskResult = riskManagementEngine.validateOrder(riskCtx);
        checks.put("riskEngine", riskResult.checks());
        if (!riskResult.passed()) {
            passed = false;
            fail.append(riskResult.failReason()).append(" ");
        }

        int mood = marketMoodService.currentScore();
        checks.put("marketMoodScore", mood);
        if (mood <= 30 && "BUY".equalsIgnoreCase(side) && riskResult.passed()) {
            passed = false;
            fail.append("시장 분위기 30 이하 신규 매수 금지. ");
        }

        ValidationResult result = new ValidationResult(passed, fail.toString().trim(), checks);
        persist(userId, stockCode, side, signalId, orderId, phase.name(), result);
        return result;
    }

    /** 주문 체결·접수 후 리스크 카운터 갱신 */
    public void onOrderAccepted(
            String userId,
            String stockCode,
            String side,
            String strategyType,
            long orderAmount,
            int quantity) {
        long amount = orderAmount > 0 ? orderAmount : estimateAmount(quantity);
        riskManagementEngine.onOrderAccepted(
                new RiskManagementEngine.OrderRiskContext(
                        userId, stockCode, side, strategyType, amount, quantity));
    }

    private static long estimateAmount(int quantity) {
        if (quantity <= 0) {
            return 0;
        }
        return (long) quantity * 50_000;
    }

    private static void check(Map<String, Object> checks, String key, boolean ok, String note) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("ok", ok);
        if (note != null) item.put("note", note);
        checks.put(key, item);
    }

    private void persist(
            String userId,
            String stockCode,
            String side,
            Long signalId,
            Long orderId,
            String phase,
            ValidationResult result) {
        OrderValidationLog log = new OrderValidationLog();
        log.setUserId(userId);
        log.setStockCode(stockCode);
        log.setSide(side);
        log.setSignalId(signalId);
        log.setOrderId(orderId);
        log.setPassed(result.passed());
        log.setValidationResult(result.passed() ? "PASS" : "FAIL");
        log.setExecutionPhase(phase);
        log.setFailReason(result.failReason());
        try {
            log.setValidationDetailJson(objectMapper.writeValueAsString(result.checks()));
            log.setChecks(log.getValidationDetailJson());
        } catch (Exception exception) {
            log.setValidationDetailJson("{}");
        }
        log.setCreatedAt(LocalDateTime.now());
        logRepository.save(log);
    }
}
