package com.noono0.stock.integration.naver.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "app.naver.news")
public class NaverNewsProperties {
    private String clientId;
    private String clientSecret;
    /** search.news.json `sort` — date | sim (공식 API 문서) */
    private String sort = "date";
}
