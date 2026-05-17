package com.noono0.stock.auth.service;

import com.noono0.stock.auth.config.PasswordResetProperties;
import com.noono0.stock.auth.domain.PasswordResetCode;
import com.noono0.stock.auth.mapper.AppUserMapper;
import com.noono0.stock.auth.repository.PasswordResetCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String GENERIC_SENT_MSG =
            "등록된 이메일이면 인증번호가 발송됩니다. 메일함을 확인해 주세요.";

    private final AppUserMapper appUserMapper;
    private final PasswordResetCodeRepository resetCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMailService authMailService;
    private final PasswordResetProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public Map<String, Object> requestCode(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        validateEmailFormat(email);

        var user = appUserMapper.findByEmail(email);
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            return Map.of("sent", true, "message", GENERIC_SENT_MSG);
        }

        enforceCooldown(user.getId());

        String code = generateSixDigitCode();
        resetCodeRepository.invalidatePending(user.getId(), LocalDateTime.now());

        PasswordResetCode entity = new PasswordResetCode();
        entity.setUserId(user.getId());
        entity.setCodeHash(passwordEncoder.encode(code));
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(properties.getCodeTtlMinutes()));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setVerifyAttempts(0);
        resetCodeRepository.save(entity);

        authMailService.sendPasswordResetCode(email, code, properties.getCodeTtlMinutes());

        return Map.of(
                "sent",
                true,
                "message",
                GENERIC_SENT_MSG,
                "devMailConfigured",
                authMailService.isConfigured());
    }

    @Transactional
    public void confirmAndReset(String rawEmail, String rawCode, String newPassword) {
        String email = normalizeEmail(rawEmail);
        validateEmailFormat(email);
        validatePassword(newPassword);

        String code = rawCode == null ? "" : rawCode.trim();
        if (!code.matches("\\d{6}")) {
            throw new IllegalArgumentException("인증번호는 6자리 숫자여야 합니다.");
        }

        var user = appUserMapper.findByEmail(email);
        if (user == null) {
            throw new IllegalStateException("이메일 또는 인증번호가 올바르지 않습니다.");
        }

        PasswordResetCode pending =
                resetCodeRepository
                        .findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "유효한 인증 요청이 없습니다. 인증번호를 다시 요청해 주세요."));

        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            markUsed(pending);
            resetCodeRepository.save(pending);
            throw new IllegalStateException("인증번호가 만료되었습니다. 다시 요청해 주세요.");
        }

        if (pending.getVerifyAttempts() >= properties.getMaxVerifyAttempts()) {
            markUsed(pending);
            resetCodeRepository.save(pending);
            throw new IllegalStateException("인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해 주세요.");
        }

        pending.setVerifyAttempts(pending.getVerifyAttempts() + 1);
        resetCodeRepository.save(pending);

        if (!passwordEncoder.matches(code, pending.getCodeHash())) {
            throw new IllegalStateException("이메일 또는 인증번호가 올바르지 않습니다.");
        }

        markUsed(pending);
        resetCodeRepository.save(pending);
        appUserMapper.updatePasswordHash(user.getId(), passwordEncoder.encode(newPassword.trim()));
        resetCodeRepository.invalidatePending(user.getId(), LocalDateTime.now());
        log.info("[PASSWORD-RESET] 비밀번호 변경 완료 userId={}", user.getId());
    }

    private void enforceCooldown(long userId) {
        resetCodeRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
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

    private String generateSixDigitCode() {
        return String.valueOf(100_000 + secureRandom.nextInt(900_000));
    }

    private static void markUsed(PasswordResetCode pending) {
        pending.setUsedAt(LocalDateTime.now());
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

    private static void validatePassword(String password) {
        String p = password == null ? "" : password.trim();
        if (p.length() < 6) {
            throw new IllegalArgumentException("비밀번호는 6자 이상이어야 합니다.");
        }
    }
}
