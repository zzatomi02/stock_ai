package com.noono0.stock.risk.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 주문 전 리스크 검사 결과 */
public class RiskCheckResult {
    private boolean passed = true;
    private final StringBuilder failReason = new StringBuilder();
    private final Map<String, Object> checks = new LinkedHashMap<>();
    private final List<String> blockCodes = new ArrayList<>();

    public boolean passed() {
        return passed;
    }

    public String failReason() {
        return failReason.toString().trim();
    }

    public Map<String, Object> checks() {
        return checks;
    }

    public List<String> blockCodes() {
        return List.copyOf(blockCodes);
    }

    public void pass(String key, Object detail) {
        putCheck(key, true, detail);
    }

    public void fail(String key, String code, String message, Object detail) {
        passed = false;
        blockCodes.add(code);
        failReason.append(message).append(" ");
        putCheck(key, false, detail != null ? detail : message);
    }

    private void putCheck(String key, boolean ok, Object detail) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("ok", ok);
        if (detail != null) {
            item.put("detail", detail);
        }
        checks.put(key, item);
    }
}
