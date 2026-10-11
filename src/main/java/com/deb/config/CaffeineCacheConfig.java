package com.deb.config;

import com.deb.cache.model.CachedLinkDto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineCacheConfig {

    @Bean
    public Cache<String, CachedLinkDto> l1LinkCache() {
        return Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}
