package com.noono0.stock.autotrade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 텔레그램·디스코드·메일(선택). 환경 변수 미설정 시 로그만. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {
    private final RestClient restClient;
    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${app.notifications.telegram-bot-token:}")
    private String telegramToken;

    @Value("${app.notifications.telegram-chat-id:}")
    private String telegramChat;

    @Value("${app.notifications.discord-webhook-url:}")
    private String discordUrl;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public void notify(String text) {
        log.info("[알림] {}", text);
        if (StringUtils.hasText(telegramToken) && StringUtils.hasText(telegramChat)) {
            try {
                String u =
                        String.format(
                                "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                                telegramToken,
                                telegramChat,
                                URLEncoder.encode(text, StandardCharsets.UTF_8));
                restClient.get().uri(u).retrieve().toBodilessEntity();
            } catch (Exception exception) {
                log.warn("telegram: {}", exception.getMessage());
            }
        }
        if (StringUtils.hasText(discordUrl)) {
            try {
                String body = "{\"content\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
                restClient.post().uri(discordUrl).header("Content-Type", "application/json").body(body).retrieve().toBodilessEntity();
            } catch (Exception exception) {
                log.warn("discord: {}", exception.getMessage());
            }
        }
        mailSender.ifAvailable(
                ms -> {
                    if (!StringUtils.hasText(mailFrom)) return;
                    try {
                        SimpleMailMessage m = new SimpleMailMessage();
                        m.setFrom(mailFrom);
                        m.setTo(mailFrom);
                        m.setSubject("[StockAI] 알림");
                        m.setText(text);
                        ms.send(m);
                    } catch (Exception exception) {
                        log.warn("mail: {}", exception.getMessage());
                    }
                });
    }
}
