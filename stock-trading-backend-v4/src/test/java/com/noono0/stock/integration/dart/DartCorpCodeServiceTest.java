package com.noono0.stock.integration.dart;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class DartCorpCodeServiceTest {

    @Test
    void normalizeStockCode_padsToSixDigits() {
        assertThat(DartCorpCodeService.normalizeStockCode("5930")).isEqualTo("005930");
        assertThat(DartCorpCodeService.normalizeStockCode("005930")).isEqualTo("005930");
    }

    @Test
    void parseCorpCodeZip_readsStockMapping() throws Exception {
        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <result>
                  <list>
                    <corp_code>00126380</corp_code>
                    <corp_name>삼성전자</corp_name>
                    <stock_code>005930</stock_code>
                  </list>
                  <list>
                    <corp_code>99999999</corp_code>
                    <corp_name>비상장</corp_name>
                    <stock_code> </stock_code>
                  </list>
                </result>
                """;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            zos.putNextEntry(new ZipEntry("CORPCODE.xml"));
            zos.write(xml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        Map<String, String> map = DartCorpCodeService.parseCorpCodeZip(bos.toByteArray());
        assertThat(map).containsEntry("005930", "00126380");
        assertThat(map).doesNotContainKey(" ");
    }
}
