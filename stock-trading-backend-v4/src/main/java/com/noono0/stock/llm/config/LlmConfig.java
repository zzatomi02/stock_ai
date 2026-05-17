package com.noono0.stock.llm.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LlmOpenAiProperties.class)
public class LlmConfig {}
