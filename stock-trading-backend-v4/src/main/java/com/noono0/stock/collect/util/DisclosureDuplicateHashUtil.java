package com.noono0.stock.collect.util;

import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

public final class DisclosureDuplicateHashUtil {
    private DisclosureDuplicateHashUtil() {}

    public static String hash(String provider, String rceptNo, String originalUrl, String reportName) {
        if (StringUtils.hasText(rceptNo)) {
            return DigestUtils.md5DigestAsHex(("rcept:" + rceptNo.trim()).getBytes(StandardCharsets.UTF_8));
        }
        if (StringUtils.hasText(originalUrl)) {
            return DigestUtils.md5DigestAsHex(("url:" + originalUrl.trim()).getBytes(StandardCharsets.UTF_8));
        }
        String basis = (provider != null ? provider : "") + "|" + (reportName != null ? reportName : "");
        return DigestUtils.md5DigestAsHex(basis.getBytes(StandardCharsets.UTF_8));
    }
}
