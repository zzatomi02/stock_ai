package com.noono0.stock.risk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.execution.ExecutionPhase;
import com.noono0.stock.execution.OrderGateway;
import com.noono0.stock.ops.trading.TradingControlService;
import com.noono0.stock.risk.domain.OrderValidationLog;
import com.noono0.stock.risk.repository.OrderValidationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RiskGate {
    private final OrderValidationEngine validationEngine;

    public record ValidationResult(boolean passed, String message, Map<String, Object> checks) {}

    public ValidationResult validateOrder(
            String userId, String stockCode, String side, Long signalId, String kisMode) {
        return validateOrder(userId, stockCode, side, signalId, kisMode, 0, null, 0);
    }

    public ValidationResult validateOrder(
            String userId,
            String stockCode,
            String side,
            Long signalId,
            String kisMode,
            long orderAmount,
            String strategyType,
            int quantity) {
        var r =
                validationEngine.validate(
                        userId, stockCode, side, signalId, null, kisMode, orderAmount, strategyType, quantity);
        return new ValidationResult(r.passed(), r.failReason(), r.checks());
    }

    public void onOrderAccepted(
            String userId,
            String stockCode,
            String side,
            String strategyType,
            long orderAmount,
            int quantity) {
        validationEngine.onOrderAccepted(userId, stockCode, side, strategyType, orderAmount, quantity);
    }
}
