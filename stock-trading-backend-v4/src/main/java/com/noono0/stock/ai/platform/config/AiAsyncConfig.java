package com.noono0.stock.ai.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AiAsyncConfig {

    @Bean(name = "aiAnalysisExecutor")
    public Executor aiAnalysisExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("ai-batch-");
        ex.initialize();
        return ex;
    }
}
