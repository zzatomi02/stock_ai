package com.noono0.stock.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 카카오 알림 — 추후 확장용 스텁 */
@Slf4j
@Component
public class KakaoNotificationChannel implements NotificationChannel {

    @Override
    public String channelId() {
        return "kakao";
    }

    @Override
    public void send(NotificationEvent event) {
        log.debug("[NOTIFY-KAKAO] (미구현) {}", event.type());
    }
}
