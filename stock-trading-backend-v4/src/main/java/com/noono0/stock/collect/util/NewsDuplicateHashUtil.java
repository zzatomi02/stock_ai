package com.noono0.stock.collect.util;

import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public final class NewsDuplicateHashUtil {
    private NewsDuplicateHashUtil() {}

    public static String hash(String originalLink, String naverLink, String title, LocalDateTime publishedAt) {
        String basis;
        if (StringUtils.hasText(originalLink)) {
            basis = "orig:" + originalLink.trim();
        } else if (StringUtils.hasText(naverLink)) {
            basis = "nav:" + naverLink.trim();
        } else {
            basis = "fallback:" + (title != null ? title.trim() : "") + "|" + (publishedAt != null ? publishedAt : "");
        }
        return DigestUtils.md5DigestAsHex(basis.getBytes(StandardCharsets.UTF_8));
    }
}
