package com.noono0.stock.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth.password-reset")
public class PasswordResetProperties {
    /** 인증 코드 유효 시간(분) */
    private int codeTtlMinutes = 10;
    /** 동일 이메일 재요청 최소 간격(초) */
    private int resendCooldownSeconds = 60;
    /** 최대 검증 실패 횟수(초과 시 코드 무효) */
    private int maxVerifyAttempts = 5;
}
