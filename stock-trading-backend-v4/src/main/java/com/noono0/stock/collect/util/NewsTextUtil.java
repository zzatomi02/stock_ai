package com.noono0.stock.collect.util;

import org.springframework.util.StringUtils;

public final class NewsTextUtil {
    private NewsTextUtil() {}

    public static String stripHtml(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return raw.replaceAll("<[^>]+>", "").replace("&quot;", "\"").replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").trim();
    }
}
