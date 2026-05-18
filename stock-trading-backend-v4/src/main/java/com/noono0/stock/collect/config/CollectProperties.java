package com.noono0.stock.collect.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.collect")
public class CollectProperties {
    private boolean newsSearchEnabled = false;
    private long newsSearchFixedDelayMs = 60_000L;
    private boolean dartDisclosureEnabled = false;
    private boolean kindRssEnabled = false;
    /** KIND 시장조치 RSS URL */
    private String kindRssUrl = "https://kind.krx.co.kr/disclosure/todaydisclosure.do?method=searchTodayDisclosureSub&currentPageSize=100";
}
