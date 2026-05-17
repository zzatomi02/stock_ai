package com.noono0.stock.integration.kis.credential.service;

import com.noono0.stock.integration.kis.config.KisProperties;
import com.noono0.stock.integration.kis.credential.domain.UserKisCredential;
import com.noono0.stock.integration.kis.credential.mapper.UserKisCredentialMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserKisCredentialService {
    public static final String HEADER_USER_ID = "X-User-Id";

    private final UserKisCredentialMapper userKisCredentialMapper;
    private final KisSecretCrypto crypto;
    private final KisProperties props;

    public record ResolvedCredential(
            String appKey,
            String appSecret,
            String accountNo,
            String productCode,
            boolean userScoped) {}

    public record UserLinkStatus(String userId, boolean paperLinked, boolean realLinked) {}

    public String requireUserId(HttpServletRequest req) {
        if (req == null) throw new IllegalStateException("사용자 정보가 없습니다. 다시 로그인 후 시도하세요.");
        String v = req.getHeader(HEADER_USER_ID);
        if (!StringUtils.hasText(v)) throw new IllegalStateException("사용자 식별자가 없습니다. 설정에서 사용자 아이디를 먼저 저장하세요.");
        return v.trim();
    }

    public UserLinkStatus status(String userId) {
        boolean paper = userKisCredentialMapper.findByUserIdAndMode(userId, "paper") != null;
        boolean real = userKisCredentialMapper.findByUserIdAndMode(userId, "real") != null;
        return new UserLinkStatus(userId, paper, real);
    }

    public void upsert(
            String userId, String mode, String appKey, String appSecret, String accountNo, String productCode) {
        String m = normalizeMode(mode);
        if (!StringUtils.hasText(appKey)) {
            throw new IllegalArgumentException("appKey 는 필수입니다.");
        }
        UserKisCredential e = userKisCredentialMapper.findByUserIdAndMode(userId, m);
        if (e == null) {
            e = new UserKisCredential();
        }
        boolean isNew = e.getId() == null;
        if (isNew && !StringUtils.hasText(appSecret)) {
            throw new IllegalArgumentException("최초 등록 시 appSecret 은 필수입니다.");
        }
        e.setUserId(userId);
        e.setMode(m);
        e.setAppKeyEnc(crypto.encrypt(appKey.trim()));
        if (StringUtils.hasText(appSecret)) {
            e.setAppSecretEnc(crypto.encrypt(appSecret.trim()));
        }
        // 갱신이고 appSecret 이 비어 있으면: 위에서 setAppSecretEnc 를 호출하지 않으므로 find 시점의 app_secret_enc가 유지됨
        e.setAccountNoEnc(crypto.encrypt(nz(accountNo)));
        e.setProductCode(StringUtils.hasText(productCode) ? productCode.trim() : "01");
        LocalDateTime now = LocalDateTime.now();
        if (isNew) {
            e.setCreatedAt(now);
            e.setUpdatedAt(now);
            userKisCredentialMapper.insert(e);
        } else {
            e.setUpdatedAt(now);
            userKisCredentialMapper.updateById(e);
        }
    }

    public void delete(String userId, String mode) {
        userKisCredentialMapper.deleteByUserIdAndMode(userId, normalizeMode(mode));
    }

    public ResolvedCredential resolveForRequest(String mode, HttpServletRequest req) {
        String m = normalizeMode(mode);
        String userId = req != null ? req.getHeader(HEADER_USER_ID) : null;
        if (StringUtils.hasText(userId)) {
            UserKisCredential e = userKisCredentialMapper.findByUserIdAndMode(userId.trim(), m);
            if (e == null) {
                throw new IllegalStateException(
                        "KIS 연동 정보가 없습니다. 설정에서 " + m + " 모드 appKey/appSecret 을 먼저 등록하세요.");
            }
            return new ResolvedCredential(
                    crypto.decrypt(e.getAppKeyEnc()),
                    crypto.decrypt(e.getAppSecretEnc()),
                    crypto.decrypt(e.getAccountNoEnc()),
                    StringUtils.hasText(e.getProductCode()) ? e.getProductCode() : "01",
                    true);
        }
        KisProperties.Credential c = "real".equalsIgnoreCase(m) ? props.getReal() : props.getPaper();
        return new ResolvedCredential(
                nz(c.getAppKey()),
                nz(c.getAppSecret()),
                nz(props.getAccountNo()),
                StringUtils.hasText(props.getProductCode()) ? props.getProductCode() : "01",
                false);
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    public static String normalizeMode(String mode) {
        return "real".equalsIgnoreCase(mode) ? "real" : "paper";
    }

    /**
     * 설정 화면: 연동 여부 + 저장된 appKey/계좌/상품코드 (앱 시크릿은 보안상 내려주지 않음, hasAppSecret 으로만 표시)
     */
    public Map<String, Object> statusForSettingsUi(String userId) {
        UserLinkStatus s = status(userId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", userId);
        m.put("paper", s.paperLinked() ? "linked" : "unlinked");
        m.put("real", s.realLinked() ? "linked" : "unlinked");
        m.put("paperForm", toForm(userId, "paper"));
        m.put("realForm", toForm(userId, "real"));
        return m;
    }

    private Map<String, Object> toForm(String userId, String mode) {
        UserKisCredential e = userKisCredentialMapper.findByUserIdAndMode(userId, mode);
        if (e == null) {
            return null;
        }
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("appKey", crypto.decrypt(e.getAppKeyEnc()));
        f.put("accountNo", crypto.decrypt(e.getAccountNoEnc()));
        f.put("productCode", StringUtils.hasText(e.getProductCode()) ? e.getProductCode() : "01");
        f.put("hasAppSecret", StringUtils.hasText(e.getAppSecretEnc()));
        return f;
    }
}
