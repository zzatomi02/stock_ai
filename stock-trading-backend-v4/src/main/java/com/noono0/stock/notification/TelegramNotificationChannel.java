package com.noono0.stock.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 텔레그램 — 토큰 설정 시 추후 HTTP 연동 */
@Slf4j
@Component
public class TelegramNotificationChannel implements NotificationChannel {

    @Override
    public String channelId() {
        return "telegram";
    }

    @Override
    public void send(NotificationEvent event) {
        log.info("[NOTIFY-TELEGRAM] (스텁) {} — {}", event.type(), event.body());
    }
}
