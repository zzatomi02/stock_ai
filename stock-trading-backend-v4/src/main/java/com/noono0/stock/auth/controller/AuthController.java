package com.noono0.stock.auth.controller;

import com.noono0.stock.auth.service.AuthService;
import com.noono0.stock.auth.service.PasswordResetService;
import com.noono0.stock.auth.service.UsernameFindService;
import com.noono0.stock.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final UsernameFindService usernameFindService;

    public record SignUpRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String email,
            String displayName) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {}

    public record PasswordResetRequest(@NotBlank String email) {}

    public record FindUsernameRequest(@NotBlank String email) {}

    public record PasswordResetConfirmRequest(
            @NotBlank String email, @NotBlank String code, @NotBlank String newPassword) {}

    @GetMapping("/check-username")
    public ApiResponse<?> checkUsername(@RequestParam("username") @NotBlank String username) {
        var result = authService.checkUsernameAvailability(username);
        return ApiResponse.ok(
                java.util.Map.of("available", result.available(), "username", result.username()),
                result.message());
    }

    @PostMapping("/signup")
    public ApiResponse<?> signUp(@RequestBody SignUpRequest body) {
        return ApiResponse.ok(
                authService.signUp(body.username(), body.password(), body.displayName(), body.email()),
                "회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest body) {
        return ApiResponse.ok(authService.login(body.username(), body.password()), "로그인되었습니다.");
    }

    @GetMapping("/me")
    public ApiResponse<?> me(HttpServletRequest req) {
        String userId = req.getHeader("X-User-Id");
        return ApiResponse.ok(authService.me(userId));
    }

    @PostMapping("/find-username")
    public ApiResponse<?> findUsername(@RequestBody FindUsernameRequest body) {
        var result = usernameFindService.sendUsernameToEmail(body.email());
        return ApiResponse.ok(result, (String) result.get("message"));
    }

    @PostMapping("/password-reset/request")
    public ApiResponse<?> requestPasswordReset(@RequestBody PasswordResetRequest body) {
        var result = passwordResetService.requestCode(body.email());
        return ApiResponse.ok(result, (String) result.get("message"));
    }

    @PostMapping("/password-reset/confirm")
    public ApiResponse<?> confirmPasswordReset(@RequestBody PasswordResetConfirmRequest body) {
        passwordResetService.confirmAndReset(body.email(), body.code(), body.newPassword());
        return ApiResponse.ok(null, "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.");
    }
}
