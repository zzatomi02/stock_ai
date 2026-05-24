package com.noono0.stock.integration.dart;

import com.noono0.stock.integration.dart.config.DartProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** 종목코드(6자리) → DART corp_code(8자리) 매핑 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DartCorpCodeService {

    private final DartProperties props;
    private final DartOpenApiClient dartClient;

    private final ConcurrentHashMap<String, String> stockToCorp = new ConcurrentHashMap<>();
    private volatile Instant loadedAt;

    public Optional<String> resolveCorpCode(String stockCode) {
        if (!StringUtils.hasText(stockCode)) {
            return Optional.empty();
        }
        String normalized = normalizeStockCode(stockCode);
        ensureLoaded();
        return Optional.ofNullable(stockToCorp.get(normalized));
    }

    public synchronized void refresh() {
        if (!props.isEnabled()) {
            return;
        }
        try {
            byte[] zip = dartClient.downloadCorpCodeZip();
            Map<String, String> parsed = parseCorpCodeZip(zip);
            stockToCorp.clear();
            stockToCorp.putAll(parsed);
            loadedAt = Instant.now();
            log.info("【DART】 corp_code 매핑 갱신 완료 종목 수={}", parsed.size());
        } catch (Exception exception) {
            log.error("【DART】 corp_code 갱신 실패: {}", exception.getMessage());
            throw new IllegalStateException("DART corp_code 갱신 실패", exception);
        }
    }

    private void ensureLoaded() {
        if (loadedAt != null && !isStale()) {
            return;
        }
        synchronized (this) {
            if (loadedAt != null && !isStale()) {
                return;
            }
            refresh();
        }
    }

    private boolean isStale() {
        if (loadedAt == null) {
            return true;
        }
        long hours = props.getCorpCodeCacheHours();
        return loadedAt.plusSeconds(hours * 3600L).isBefore(Instant.now());
    }

    public static String normalizeStockCode(String stockCode) {
        String digits = stockCode.replaceAll("\\D", "");
        if (digits.length() > 6) {
            digits = digits.substring(digits.length() - 6);
        }
        return String.format("%6s", digits).replace(' ', '0');
    }

    static Map<String, String> parseCorpCodeZip(byte[] zipBytes) throws Exception {
        Map<String, String> map = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.getName().toLowerCase().endsWith(".xml")) {
                    continue;
                }
                map.putAll(parseCorpCodeXml(zis));
                break;
            }
        }
        return map;
    }

    static Map<String, String> parseCorpCodeXml(InputStream xmlIn) throws Exception {
        Map<String, String> map = new HashMap<>();
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XMLStreamReader reader = factory.createXMLStreamReader(xmlIn);
        String corpCode = null;
        String stockCode = null;
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String local = reader.getLocalName();
                if ("corp_code".equals(local)) {
                    corpCode = reader.getElementText().trim();
                } else if ("stock_code".equals(local)) {
                    stockCode = reader.getElementText().trim();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "list".equals(reader.getLocalName())) {
                if (StringUtils.hasText(corpCode) && StringUtils.hasText(stockCode) && stockCode.length() == 6) {
                    map.put(stockCode, corpCode);
                }
                corpCode = null;
                stockCode = null;
            }
        }
        reader.close();
        return map;
    }
}
