package com.noono0.stock.execution.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ExecutionPhaseProperties.class)
public class ExecutionConfig {}
