package com.noono0.stock.notification;

/** 알림 채널 (이메일·텔레그램·카카오 등) */
public interface NotificationChannel {
    String channelId();

    void send(NotificationEvent event);
}
