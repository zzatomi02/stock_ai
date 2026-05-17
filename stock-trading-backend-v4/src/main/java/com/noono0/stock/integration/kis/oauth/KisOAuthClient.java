package com.noono0.stock.integration.kis.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.integration.kis.config.KisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 한국투자 OpenAPI OAuth2 (client_credentials) · 토큰 캐시.
 * 모의: openapivts.koreainvestment.com:29443 / 실전: openapi.koreainvestment.com:9443
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisOAuthClient {
    private static final String PAPER_BASE = "https://openapivts.koreainvestment.com:29443";
    private static final String REAL_BASE = "https://openapi.koreainvestment.com:9443";

    private final RestClient restClient;
    private final KisProperties kisProperties;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    public String accessToken(String mode) {
        KisProperties.Credential cred =
                "real".equalsIgnoreCase(mode) ? kisProperties.getReal() : kisProperties.getPaper();
        return accessToken(mode, cred.getAppKey(), cred.getAppSecret());
    }

    public String accessToken(String mode, String appKey, String appSecret) {
        String cacheKey = mode + ":" + (StringUtils.hasText(appKey) ? Integer.toHexString(appKey.hashCode()) : "none");
        Cached c = cache.computeIfAbsent(cacheKey, m -> new Cached());
        String safeMode = "real".equalsIgnoreCase(mode) ? "real" : "paper";
        synchronized (c) {
            if (c.token != null && c.expiresAt != null && Instant.now().isBefore(c.expiresAt)) {
                log.debug(
                        "【KIS-TOKEN】 캐시 HIT mode={} 만료≈{} (재발급 없음)",
                        safeMode,
                        c.expiresAt);
                return c.token;
            }
            boolean appKeyEmpty = !StringUtils.hasText(appKey);
            boolean appSecretEmpty = !StringUtils.hasText(appSecret);
            if (appKeyEmpty || appSecretEmpty) {
                log.error(
                        "【KIS-TOKEN】 ★ 자격 증명 누락 ★ mode={} appkey비었음={} appsecret비었음={} (시크릿 값은 로그에 남기지 않음) — 모의: KIS_PAPER_APPKEY/KIS_PAPER_SECRET, 실전: KIS_REAL_APPKEY/KIS_REAL_SECRET 또는 app.kis.*",
                        safeMode,
                        appKeyEmpty,
                        appSecretEmpty);
                throw new IllegalStateException("KIS " + safeMode + " 앱키/시크릿이 비어 있습니다.");
            }
            String base = "real".equalsIgnoreCase(safeMode) ? REAL_BASE : PAPER_BASE;
            log.warn(
                    "【KIS-TOKEN】 ▶▶▶ 신규 발급 요청 mode={} endpoint={}/oauth2/tokenP",
                    safeMode,
                    base);
            Map<String, String> payload =
                    Map.of(
                            "grant_type", "client_credentials",
                            "appkey", appKey,
                            "appsecret", appSecret);

            final String json;
            try {
                json =
                        restClient
                                .post()
                                .uri(base + "/oauth2/tokenP")
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(payload)
                                .retrieve()
                                .body(String.class);
            } catch (RestClientResponseException ex) {
                String errBody = responseBodyUtf8(ex);
                log.error(
                        "【KIS-TOKEN】 ★ HTTP 실패 ★ mode={} status={} reason={} 응답본문={}",
                        safeMode,
                        ex.getStatusCode().value(),
                        ex.getStatusText(),
                        errBody);
                throw new IllegalStateException(
                        "KIS 토큰 발급 HTTP " + ex.getStatusCode().value() + ": " + errBody,
                        ex);
            }
            try {
                JsonNode n = objectMapper.readTree(json);
                String tok = n.path("access_token").asText(null);
                int expSec = n.path("expires_in").asInt(86400);
                if (!StringUtils.hasText(tok)) {
                    log.error("【KIS-TOKEN】 ★ 응답에 access_token 없음 body={}", json);
                    throw new IllegalStateException("KIS 토큰 응답 파싱 실패: " + json);
                }
                c.token = tok;
                c.expiresAt = Instant.now().plusSeconds(Math.max(60, expSec - 120));
                String prefix = tok.length() > 12 ? tok.substring(0, 12) + "…" : "(짧음)";
                log.warn(
                        "【KIS-TOKEN】 ★★★ 발급 성공 ★★★ mode={} expires_in={}초 (캐시~{}초) token_prefix={}",
                        safeMode,
                        expSec,
                        Math.max(60, expSec - 120),
                        prefix);
                return tok;
            } catch (Exception e) {
                log.error(
                        "【KIS-TOKEN】 ★ 예외 ★ mode={} — {}",
                        safeMode,
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                if (e instanceof RuntimeException re) throw re;
                throw new IllegalStateException(e);
            }
        }
    }

    public String restBase(String mode) {
        return "real".equalsIgnoreCase(mode) ? REAL_BASE : PAPER_BASE;
    }

    private static String responseBodyUtf8(RestClientResponseException ex) {
        try {
            return ex.getResponseBodyAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ex.getMessage() != null ? ex.getMessage() : "";
        }
    }

    private static final class Cached {
        volatile String token;
        volatile Instant expiresAt;
    }
}
