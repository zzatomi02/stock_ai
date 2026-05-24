package com.noono0.stock.auth.service;

import com.noono0.stock.auth.domain.AppUser;
import com.noono0.stock.auth.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;

    public record AuthUserView(String userId, String username, String displayName, String email) {}

    public record UsernameCheckResult(boolean available, String username, String message) {}

    /** 아이디 중복·형식 검사 (회원가입 전) */
    public UsernameCheckResult checkUsernameAvailability(String username) {
        String u = normalizeUsername(username);
        boolean available = appUserMapper.findByUsername(u) == null;
        String message = available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.";
        return new UsernameCheckResult(available, u, message);
    }

    public AuthUserView signUp(String username, String password, String displayName, String email) {
        String u = normalizeUsername(username);
        validatePassword(password);
        String normalizedEmail = normalizeEmail(email);
        if (appUserMapper.findByUsername(u) != null) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }
        if (appUserMapper.findByEmail(normalizedEmail) != null) {
            throw new IllegalStateException("이미 사용 중인 이메일입니다.");
        }
        AppUser e = new AppUser();
        e.setUsername(u);
        e.setPasswordHash(passwordEncoder.encode(password));
        e.setDisplayName(StringUtils.hasText(displayName) ? displayName.trim() : u);
        e.setEmail(normalizedEmail);
        e.setCreatedAt(LocalDateTime.now());
        appUserMapper.insert(e);
        return toView(e);
    }

    public AuthUserView login(String username, String password) {
        String u = normalizeUsername(username);
        AppUser user = appUserMapper.findByUsername(u);
        if (user == null) {
            throw new IllegalStateException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalStateException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return toView(user);
    }

    public AuthUserView me(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        long id;
        try {
            id = Long.parseLong(userId.trim());
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalStateException("유효하지 않은 사용자입니다.");
        }
        AppUser user = appUserMapper.findById(id);
        if (user == null) {
            throw new IllegalStateException("사용자를 찾을 수 없습니다.");
        }
        return toView(user);
    }

    private static String normalizeUsername(String username) {
        String u = username == null ? "" : username.trim();
        if (u.length() < 3 || u.length() > 40) {
            throw new IllegalArgumentException("아이디는 3~40자로 입력하세요.");
        }
        return u;
    }

    private static void validatePassword(String password) {
        String p = password == null ? "" : password.trim();
        if (p.length() < 6) {
            throw new IllegalArgumentException("비밀번호는 6자 이상이어야 합니다.");
        }
    }

    private static String normalizeEmail(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("이메일을 입력하세요. 비밀번호 찾기에 사용됩니다.");
        }
        String email = raw.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
        return email;
    }

    private static AuthUserView toView(AppUser user) {
        return new AuthUserView(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail());
    }
}
