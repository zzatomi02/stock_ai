package com.noono0.stock.auth.service;

import com.noono0.stock.auth.config.PasswordResetProperties;
import com.noono0.stock.auth.domain.AuthEmailRequestLog;
import com.noono0.stock.auth.domain.AuthEmailRequestType;
import com.noono0.stock.auth.mapper.AppUserMapper;
import com.noono0.stock.auth.repository.AuthEmailRequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsernameFindService {
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String GENERIC_SENT_MSG =
            "등록된 이메일이면 아이디 안내 메일이 발송됩니다. 메일함을 확인해 주세요.";

    private final AppUserMapper appUserMapper;
    private final AuthMailService authMailService;
    private final AuthEmailRequestLogRepository requestLogRepository;
    private final PasswordResetProperties properties;

    @Transactional
    public Map<String, Object> sendUsernameToEmail(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        validateEmailFormat(email);

        enforceCooldown(email);

        var user = appUserMapper.findByEmail(email);
        if (user != null && StringUtils.hasText(user.getUsername())) {
            authMailService.sendFindUsername(email, user.getUsername(), user.getDisplayName());
            log.info("[FIND-USERNAME] 아이디 안내 메일 발송 email={}", email);
        }

        saveLog(email);

        return Map.of(
                "sent",
                true,
                "message",
                GENERIC_SENT_MSG,
                "devMailConfigured",
                authMailService.isConfigured());
    }

    private void enforceCooldown(String email) {
        requestLogRepository
                .findFirstByEmailAndRequestTypeOrderByCreatedAtDesc(email, AuthEmailRequestType.FIND_USERNAME)
                .ifPresent(
                        last -> {
                            LocalDateTime next =
                                    last.getCreatedAt()
                                            .plusSeconds(properties.getResendCooldownSeconds());
                            if (next.isAfter(LocalDateTime.now())) {
                                long sec =
                                        java.time.Duration.between(LocalDateTime.now(), next)
                                                .getSeconds();
                                throw new IllegalStateException(
                                        "잠시 후 다시 요청해 주세요. (" + Math.max(1, sec) + "초)");
                            }
                        });
    }

    private void saveLog(String email) {
        AuthEmailRequestLog log = new AuthEmailRequestLog();
        log.setEmail(email);
        log.setRequestType(AuthEmailRequestType.FIND_USERNAME);
        log.setCreatedAt(LocalDateTime.now());
        requestLogRepository.save(log);
    }

    private static String normalizeEmail(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("이메일을 입력하세요.");
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static void validateEmailFormat(String email) {
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
    }
}
