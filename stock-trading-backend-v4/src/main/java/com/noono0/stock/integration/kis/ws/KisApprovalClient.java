package com.noono0.stock.integration.kis.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.noono0.stock.integration.kis.config.KisProperties;
import com.noono0.stock.integration.kis.oauth.KisOAuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * WebSocket 접속용 승인키(approval_key). REST OAuth 토큰과 별도입니다.
 *
 * @see <a href="https://apiportal.koreainvestment.com/apiservice-apiservice%3F/oauth2/Approval">KIS Approval</a>
 */
@Component
@RequiredArgsConstructor
public class KisApprovalClient {

    private final RestClient restClient;
    private final KisOAuthClient oauth;
    private final KisProperties kisProperties;
    private final ObjectMapper objectMapper;

    public String approvalKey(String mode) {
        KisProperties.Credential cred =
                "real".equalsIgnoreCase(mode) ? kisProperties.getReal() : kisProperties.getPaper();
        if (!StringUtils.hasText(cred.getAppKey()) || !StringUtils.hasText(cred.getAppSecret())) {
            throw new IllegalStateException("KIS " + mode + " 앱키/시크릿이 비어 있습니다.");
        }
        String base = oauth.restBase(mode);
        String json = postApprovalJson(base, cred);
        try {
            JsonNode n = objectMapper.readTree(json);
            String key = n.path("approval_key").asText(null);
            if (!StringUtils.hasText(key)) {
                key = n.path("output").path("approval_key").asText(null);
            }
            if (!StringUtils.hasText(key)) {
                throw new IllegalStateException("approval_key 파싱 실패: " + json);
            }
            return key;
        } catch (Exception exception) {
            if (exception instanceof RuntimeException re) throw re;
            throw new IllegalStateException(exception);
        }
    }

    /** 문서는 JSON이 일반적이나, 환경에 따라 form이 필요할 수 있어 보조 시도 */
    private String postApprovalJson(String base, KisProperties.Credential cred) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("grant_type", "client_credentials");
        body.put("appkey", cred.getAppKey());
        body.put("secretkey", cred.getAppSecret());
        try {
            return restClient
                    .post()
                    .uri(base + "/oauth2/Approval")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);
        } catch (Exception jsonRequestException) {
            try {
                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("grant_type", "client_credentials");
                form.add("appkey", cred.getAppKey());
                form.add("secretkey", cred.getAppSecret());
                return restClient
                        .post()
                        .uri(base + "/oauth2/Approval")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .body(String.class);
            } catch (Exception formRequestException) {
                formRequestException.addSuppressed(jsonRequestException);
                throw new IllegalStateException(
                        "KIS Approval 호출 실패: " + formRequestException.getMessage(), formRequestException);
            }
        }
    }
}
