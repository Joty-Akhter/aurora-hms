package com.easyops.hospitalcorporatediscount.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_COVERAGE_RULES = "coverageRules";
    public static final String CACHE_DISCOUNT_SCHEME = "discountScheme";
    public static final String CACHE_ACTIVE_DISCOUNT_SCHEMES = "activeDiscountSchemes";

    private static final int MAX_SIZE = 500;
    private static final int EXPIRE_SECONDS = 60;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                CACHE_COVERAGE_RULES,
                CACHE_DISCOUNT_SCHEME,
                CACHE_ACTIVE_DISCOUNT_SCHEMES
        );
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(MAX_SIZE)
                .expireAfterWrite(EXPIRE_SECONDS, TimeUnit.SECONDS));
        return manager;
    }
}
