package com.noono0.stock.news.parser;

import java.time.LocalDateTime;

public record ParsedNewsArticle(
        String title,
        String fullBody,
        LocalDateTime publishedAt
) {
}
