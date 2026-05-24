package com.noono0.stock.integration.dart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noono0.stock.integration.dart.config.DartProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/** DART 전자공시 Open API (list.json, corpCode.xml) */
@Slf4j
@Component
@RequiredArgsConstructor
public class DartOpenApiClient {

    private final DartProperties props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public void ensureConfigured() {
        if (!StringUtils.hasText(props.getApiKey())) {
            throw new IllegalStateException("DART_API_KEY 미설정 (환경변수 또는 application-secrets.local.yml)");
        }
    }

    public byte[] downloadCorpCodeZip() {
        ensureConfigured();
        String url =
                UriComponentsBuilder.fromHttpUrl(props.getBaseUrl() + "/corpCode.xml")
                        .queryParam("crtfc_key", props.getApiKey())
                        .build()
                        .toUriString();
        log.info("【DART】 corpCode.xml 다운로드");
        return restClient.get().uri(url).retrieve().body(byte[].class);
    }

    public JsonNode fetchDisclosureList(String corpCode, String bgnDe, String endDe, int pageNo, int pageCount) {
        ensureConfigured();
        String url =
                UriComponentsBuilder.fromHttpUrl(props.getBaseUrl() + "/list.json")
                        .queryParam("crtfc_key", props.getApiKey())
                        .queryParam("corp_code", corpCode)
                        .queryParam("bgn_de", bgnDe)
                        .queryParam("end_de", endDe)
                        .queryParam("page_no", pageNo)
                        .queryParam("page_count", pageCount)
                        .build()
                        .toUriString();
        try {
            String json = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode tree = objectMapper.readTree(json);
            String status = tree.path("status").asText("");
            if (!"000".equals(status) && !"013".equals(status)) {
                log.warn(
                        "【DART】 list.json 비정상 status={} message={}",
                        status,
                        tree.path("message").asText());
            }
            return tree;
        } catch (Exception exception) {
            log.error("【DART】 list.json 실패 corp={}: {}", corpCode, exception.getMessage());
            throw new IllegalStateException("DART 공시 목록 조회 실패: " + exception.getMessage(), exception);
        }
    }
}
