package com.noono0.stock.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final List<NotificationChannel> channels;

    public void publish(NotificationEvent event) {
        for (NotificationChannel ch : channels) {
            if (!"kakao".equals(ch.channelId())) {
                ch.send(event);
            }
        }
    }
}
