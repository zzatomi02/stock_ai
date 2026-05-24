package com.noono0.stock.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class EmailNotificationChannel implements NotificationChannel {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.notifications.alert-email:}")
    private String alertEmail;

    @Value("${app.notifications.email-enabled:false}")
    private boolean emailEnabled;

    @Override
    public String channelId() {
        return "email";
    }

    @Override
    public void send(NotificationEvent event) {
        if (!emailEnabled || mailSender == null || !StringUtils.hasText(alertEmail)) {
            log.info("[NOTIFY-EMAIL] (비활성) {} — {}", event.type(), event.body());
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(alertEmail);
            msg.setSubject("[Stock] " + event.title());
            msg.setText(event.body());
            mailSender.send(msg);
            log.info("[NOTIFY-EMAIL] 발송 완료 type={}", event.type());
        } catch (Exception exception) {
            log.warn("[NOTIFY-EMAIL] 발송 실패: {}", exception.getMessage());
        }
    }
}
