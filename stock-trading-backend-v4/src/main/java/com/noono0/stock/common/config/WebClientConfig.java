package com.noono0.stock.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public RestClient restClient() { return RestClient.builder().build(); }
    @Bean
    public WebClient webClient() { return WebClient.builder().build(); }
}
