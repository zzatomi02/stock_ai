package com.noono0.stock.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthMailService {
    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public boolean isConfigured() {
        return mailSender.getIfAvailable() != null && StringUtils.hasText(mailFrom);
    }

    public void sendPasswordResetCode(String toEmail, String code, int ttlMinutes) {
        String body =
                """
                안녕하세요.

                비밀번호 재설정을 요청하셨습니다.
                아래 6자리 인증번호를 입력해 주세요.

                인증번호: %s
                유효 시간: %d분

                본인이 요청하지 않았다면 이 메일을 무시하세요.
                """
                        .formatted(code, ttlMinutes);
        sendMail(
                toEmail,
                "[StockAI] 비밀번호 재설정 인증번호",
                body,
                "PASSWORD-RESET",
                () -> log.warn("[PASSWORD-RESET] 메일 미설정 — to={} code={}", toEmail, code));
    }

    public void sendFindUsername(String toEmail, String username, String displayName) {
        String nameLine =
                StringUtils.hasText(displayName) && !displayName.equals(username)
                        ? "표시 이름: %s%n".formatted(displayName.trim())
                        : "";
        String body =
                """
                안녕하세요.

                아이디 찾기를 요청하셨습니다.
                %s아이디: %s

                로그인 후 비밀번호를 잊으셨다면 '비밀번호 찾기'를 이용해 주세요.
                본인이 요청하지 않았다면 이 메일을 무시하세요.
                """
                        .formatted(nameLine, username);
        sendMail(
                toEmail,
                "[StockAI] 아이디 찾기 안내",
                body,
                "FIND-USERNAME",
                () ->
                        log.warn(
                                "[FIND-USERNAME] 메일 미설정 — to={} username={}",
                                toEmail,
                                username));
    }

    private void sendMail(
            String toEmail,
            String subject,
            String body,
            String logTag,
            Runnable onNotConfigured) {
        if (!isConfigured()) {
            onNotConfigured.run();
            return;
        }
        JavaMailSender ms = mailSender.getIfAvailable();
        if (ms == null) {
            log.warn("[{}] JavaMailSender 없음 — to={}", logTag, toEmail);
            return;
        }
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(mailFrom);
        m.setTo(toEmail);
        m.setSubject(subject);
        m.setText(body);
        try {
            ms.send(m);
            log.info("[{}] 메일 발송: to={}", logTag, toEmail);
        } catch (MailAuthenticationException e) {
            log.warn("[{}] SMTP 인증 실패: {}", logTag, e.getMessage());
            throw new IllegalStateException(gmailAuthHelpMessage(), e);
        } catch (Exception e) {
            log.warn("[{}] 메일 발송 실패: {}", logTag, e.getMessage());
            throw new IllegalStateException(
                    "메일을 보내지 못했습니다. MAIL_HOST·MAIL_USERNAME·MAIL_PASSWORD 설정을 확인하세요.", e);
        }
    }

    private static String gmailAuthHelpMessage() {
        return """
                Gmail SMTP 인증에 실패했습니다. 일반 로그인 비밀번호는 사용할 수 없습니다.
                1) Google 계정 → 보안 → 2단계 인증 켜기
                2) 앱 비밀번호 생성 (메일 / 기타)
                3) stock/.env 의 MAIL_PASSWORD 를 16자리 앱 비밀번호로 변경 후 backend 재시작
                """.trim();
    }
}
