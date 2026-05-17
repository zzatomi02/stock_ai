package com.noono0.stock.integration.dart.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.dart")
public class DartProperties {
    private String apiKey = "";
    private String baseUrl = "https://opendart.fss.or.kr/api";
    private boolean enabled = true;
    /** corpCode.xml 캐시 유효 시간(시간) */
    private int corpCodeCacheHours = 24;
    private int defaultLookbackDays = 7;
    private int maxItemsPerStock = 30;
}
