package com.noono0.stock.integration.kind;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** KIND/KRX 시장조치·공시 RSS/XML 파싱 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KindRssClient {
    private final RestClient restClient;

    public record KindRssItem(String title, String link, String description, String pubDate) {}

    public List<KindRssItem> fetch(String rssUrl) {
        String body =
                restClient.get().uri(rssUrl).retrieve().body(String.class);
        if (body == null || body.isBlank()) {
            return List.of();
        }
        try {
            Document doc =
                    DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder()
                            .parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
            NodeList items = doc.getElementsByTagName("item");
            List<KindRssItem> result = new ArrayList<>();
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                result.add(
                        new KindRssItem(
                                text(item, "title"),
                                text(item, "link"),
                                text(item, "description"),
                                text(item, "pubDate")));
            }
            return result;
        } catch (Exception e) {
            log.warn("KIND RSS 파싱 실패: {}", e.getMessage());
            throw new IllegalStateException("KIND RSS 파싱 실패: " + e.getMessage(), e);
        }
    }

    private static String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return "";
        }
        return nodes.item(0).getTextContent();
    }
}
