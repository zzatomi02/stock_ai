package com.noono0.stock.news.parser;

import com.noono0.stock.news.domain.NewsSource;

import java.util.List;

public interface NewsSourceParser {
    String parserKey();

    List<String> extractArticleUrls(NewsSource source, String listHtml);

    ParsedNewsArticle parseArticle(NewsSource source, String articleUrl, String articleHtml);
}
