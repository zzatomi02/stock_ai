package com.noono0.stock.notification;

import com.noono0.stock.risk.domain.enums.RiskEventType;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationEvent(
        NotificationEventType type,
        String title,
        String body,
        String stockCode,
        String strategyType,
        LocalDateTime createdAt,
        Map<String, Object> metadata) {

    public static NotificationEvent fromRisk(
            RiskEventType riskType, String message, String stockCode, String strategyType) {
        NotificationEventType mapped =
                switch (riskType) {
                    case DAILY_LOSS_LIMIT_REACHED, DAILY_LOSS_AMOUNT_REACHED -> NotificationEventType.DAILY_LOSS_LIMIT_REACHED;
                    case STRATEGY_AUTO_DISABLED -> NotificationEventType.STRATEGY_AUTO_DISABLED;
                    case API_ERROR_ORDER_HALT, QUOTE_DELAY_HALT -> NotificationEventType.API_ERROR;
                    default -> NotificationEventType.RISK_EXIT_TRIGGERED;
                };
        return new NotificationEvent(
                mapped,
                riskType.name(),
                message,
                stockCode,
                strategyType,
                LocalDateTime.now(),
                Map.of("riskEventType", riskType.name()));
    }
}
