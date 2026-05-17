package com.noono0.stock.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

/**
 * {@code app.redis.enabled=true} 일 때만 수동 Bean 등록 (메인에서 Redis 자동설정 제외).
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisInfraConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password) {
        RedisStandaloneConfiguration c = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) c.setPassword(password);
        return new LettuceConnectionFactory(c);
    }

    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory f) {
        RedisCacheConfiguration def =
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(30));
        return RedisCacheManager.builder(f)
                .cacheDefaults(def)
                .transactionAware()
                .build();
    }
}
