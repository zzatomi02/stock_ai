package com.noono0.stock.integration.naver.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NaverNewsProperties.class)
public class NaverNewsConfig {}
