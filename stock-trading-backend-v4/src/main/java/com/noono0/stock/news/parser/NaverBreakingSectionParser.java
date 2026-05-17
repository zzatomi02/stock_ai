package com.noono0.stock.news.parser;

import com.noono0.stock.news.domain.NewsSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class NaverBreakingSectionParser implements NewsSourceParser {
    public static final String KEY = "NAVER_BREAKING_SECTION";

    @Override
    public String parserKey() {
        return KEY;
    }

    @Override
    public List<String> extractArticleUrls(NewsSource source, String listHtml) {
        Document doc = Jsoup.parse(listHtml, source.getBaseUrl());
        Set<String> links = new LinkedHashSet<>();
        for (Element a : doc.select("a[href*=/article/]")) {
            String href = a.absUrl("href");
            if (!StringUtils.hasText(href)) continue;
            if (!href.contains("news.naver.com")) continue;
            links.add(stripParams(href));
        }
        return new ArrayList<>(links);
    }

    @Override
    public ParsedNewsArticle parseArticle(NewsSource source, String articleUrl, String articleHtml) {
        Document doc = Jsoup.parse(articleHtml, articleUrl);
        String title = firstText(doc,
                "meta[property=og:title]",
                "h2#title_area",
                "h2.media_end_head_headline");
        String body = firstText(doc,
                "article#dic_area",
                "div#dic_area",
                "article#newsct_article",
                "div#newsct_article");
        LocalDateTime publishedAt = parseDate(firstText(doc,
                "meta[property=article:published_time]",
                "span.media_end_head_info_datestamp_time",
                "span#newsct_article_info_datestamp_time"));
        return new ParsedNewsArticle(clean(title), clean(body), publishedAt);
    }

    private static String stripParams(String url) {
        int idx = url.indexOf('?');
        return idx >= 0 ? url.substring(0, idx) : url;
    }

    private static String firstText(Document doc, String... selectors) {
        for (String s : selectors) {
            Element e = doc.selectFirst(s);
            if (e == null) continue;
            String value = "meta[property=og:title]".equals(s)
                    ? e.attr("content")
                    : e.text();
            if (StringUtils.hasText(value)) return value;
        }
        return "";
    }

    private static String clean(String raw) {
        return raw == null ? "" : raw.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static LocalDateTime parseDate(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            return OffsetDateTime.parse(raw).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy.MM.dd. a h:mm"));
        } catch (Exception ignored) {
        }
        return null;
    }
}
