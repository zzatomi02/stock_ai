package com.noono0.stock.broker.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.noono0.stock.broker.dto.DailyCcnlRowDto;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * KIS v1_국내주식-0xx 일별체결(조회) 응답 output1 배열을 행 DTO로 변환. 필드명이 모드/버전에 따라 달라질 수 있어
 * 후보 키를 순서대로 시도한다.
 */
public final class KisCcnlOutputParser {
    private KisCcnlOutputParser() {}

    public static List<DailyCcnlRowDto> parseRows(JsonNode root, ObjectMapper om) {
        if (root == null) {
            return List.of();
        }
        if (!"0".equals(root.path("rt_cd").asText(""))) {
            return List.of();
        }
        JsonNode arr = root.get("output1");
        if (arr == null || !arr.isArray() || arr.isEmpty()) {
            return List.of();
        }
        List<DailyCcnlRowDto> out = new ArrayList<>();
        for (JsonNode row : arr) {
            if (!row.isObject()) {
                continue;
            }
            String od = firstText(row, "ord_dt", "ORD_DT", "ccld_dt", "CCLD_DT");
            String ot = firstText(row, "ord_tmd", "ORD_TMD", "ccld_tmd", "CCLD_TMD", "ccld_dtm", "CCLD_DTM");
            String sc = firstText(row, "shtn_pdno", "SHTN_PDNO", "pdno", "PDNO", "stck_shrn_iscd", "STCK_SHRN_ISCD");
            String name = firstText(row, "prdt_name", "PRDT_NAME", "ord_item_name", "ORD_ITEM_NAME", "isnm", "ISNM");
            String sb = firstText(row, "sll_buy_dvsn_cd", "SLL_BUY_DVSN_CD", "sll_buy_dvsn", "SLL_BUY_DVSN");
            long qty = firstLong(row, "ccld_qty", "CCLD_QTY", "ord_qty", "ORD_QTY", "ccld_qty_smtl", "CCLD_QTY_SMTL");
            long pr = firstLong(row, "ccld_unpr", "CCLD_UNPR", "ccld_prpr", "CCLD_PRPR", "unpr", "UNPR", "ccld_avg_unpr2", "CCLD_AVG_UNPR2");
            out.add(
                    new DailyCcnlRowDto(
                            od,
                            fixTime6(ot),
                            sc,
                            name,
                            sideKorean(sb),
                            sb,
                            qty,
                            pr,
                            compactRow(row, om)));
        }
        return out;
    }

    private static String sideKorean(String code) {
        if (!StringUtils.hasText(code)) {
            return "";
        }
        String c = code.trim();
        if ("1".equals(c) || "01".equals(c)) {
            return "매수";
        }
        if ("2".equals(c) || "02".equals(c)) {
            return "매도";
        }
        if ("3".equals(c)) {
            return "전매도";
        }
        return c;
    }

    private static String firstText(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode c = n.get(k);
            if (c != null && !c.isNull()) {
                String s = c.asText("").trim();
                if (StringUtils.hasText(s)) {
                    return s;
                }
            }
        }
        return "";
    }

    private static long firstLong(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode c = n.get(k);
            if (c != null && c.isNumber()) {
                return c.asLong(0L);
            }
            if (c != null && c.isTextual()) {
                String t = c.asText("").replace(",", "").trim();
                if (StringUtils.hasText(t)) {
                    try {
                        return Long.parseLong(t);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return 0L;
    }

    /** HHMMSS → HH:mm:ss (가능한 경우) */
    private static String fixTime6(String t) {
        if (!StringUtils.hasText(t) || t.length() < 4) {
            return t;
        }
        String d = t.replaceAll("[^0-9]", "");
        if (d.length() >= 6) {
            d = d.substring(0, 6);
            return d.substring(0, 2) + ":" + d.substring(2, 4) + ":" + d.substring(4, 6);
        }
        return t;
    }

    private static final Set<String> KEEP_KEYS = Set.of(
            "odno",
            "ODNO",
            "orgn_odno",
            "ORGN_ODNO",
            "sll_buy_dvsn_cd",
            "SLL_BUY_DVSN_CD",
            "ccld_qty",
            "CCLD_QTY",
            "ccld_unpr",
            "CCLD_UNPR",
            "stck_shrn_iscd",
            "STCK_SHRN_ISCD",
            "shtn_pdno",
            "SHTN_PDNO",
            "prdt_name",
            "PRDT_NAME",
            "ord_tmd",
            "ORD_TMD",
            "ord_dt",
            "ORD_DT",
            "ccld_tmd",
            "CCLD_TMD",
            "ccld_smtl_amt",
            "CCLD_SMTL_AMT",
            "tot_ccld_amt",
            "TOT_CCLD_AMT");

    private static String compactRow(JsonNode row, ObjectMapper om) {
        if (!(row instanceof ObjectNode o)) {
            return "";
        }
        ObjectNode c = om.createObjectNode();
        o.fields()
                .forEachRemaining(
                        e -> {
                            if (KEEP_KEYS.contains(e.getKey())) {
                                c.set(e.getKey(), e.getValue());
                            }
                        });
        try {
            return om.writeValueAsString(c);
        } catch (JsonProcessingException e) {
            return "";
        }
    }
}
