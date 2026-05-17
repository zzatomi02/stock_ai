package com.noono0.stock.notification;

public enum NotificationEventType {
    BUY_SIGNAL_CREATED,
    SELL_SIGNAL_CREATED,
    RISK_EXIT_TRIGGERED,
    DAILY_LOSS_LIMIT_REACHED,
    STRATEGY_AUTO_DISABLED,
    API_ERROR,
    NEWS_BAD_KEYWORD_DETECTED
}
