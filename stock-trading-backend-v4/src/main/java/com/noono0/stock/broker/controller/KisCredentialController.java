package com.noono0.stock.broker.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.integration.kis.credential.service.UserKisCredentialService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/broker/kis/credentials")
@RequiredArgsConstructor
public class KisCredentialController {
    private final UserKisCredentialService credentialService;

    public record UpsertCredentialRequest(
            String mode, String appKey, String appSecret, String accountNo, String productCode) {}

    @GetMapping("/status")
    public ApiResponse<?> status(HttpServletRequest req) {
        String userId = credentialService.requireUserId(req);
        return ApiResponse.ok(credentialService.statusForSettingsUi(userId));
    }

    @PostMapping
    public ApiResponse<?> upsert(@RequestBody UpsertCredentialRequest body, HttpServletRequest req) {
        String userId = credentialService.requireUserId(req);
        credentialService.upsert(
                userId, body.mode(), body.appKey(), body.appSecret(), body.accountNo(), body.productCode());
        return ApiResponse.ok(Map.of("saved", true), "KIS 연동 정보가 저장되었습니다.");
    }

    @DeleteMapping
    public ApiResponse<?> delete(@RequestParam("mode") String mode, HttpServletRequest req) {
        String userId = credentialService.requireUserId(req);
        credentialService.delete(userId, mode);
        return ApiResponse.ok(Map.of("deleted", true), "KIS 연동 정보가 삭제되었습니다.");
    }
}
