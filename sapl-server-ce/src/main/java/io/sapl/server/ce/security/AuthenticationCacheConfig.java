package io.sapl.server.ce.security;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
public class AuthenticationCacheConfig {

    @Value("${io.sapl.server.apiKeyCaching.enabled:#{False}}")
    private boolean apiKeyCachingEnabled;

    @Value("${io.sapl.server.apiKeyCaching.expire:#{300}}")
    private Integer apiKeyCachingExpireSeconds;

    @Value("${io.sapl.server.apiKeyCaching.maxSize:#{300}}")
    private Integer apiKeyCachingMaxSize;

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder().expireAfterAccess(apiKeyCachingExpireSeconds, TimeUnit.SECONDS).initialCapacity(10)
                .maximumSize(apiKeyCachingMaxSize);
    }

    @Bean
    public CacheManager cacheManager(Caffeine caffeine) {
        if (apiKeyCachingEnabled) {
            CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
            caffeineCacheManager.setCaffeine(caffeine);
            return caffeineCacheManager;
        } else {
            return new NoOpCacheManager();
        }
    }

}
