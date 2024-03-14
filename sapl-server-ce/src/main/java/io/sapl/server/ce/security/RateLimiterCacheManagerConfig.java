package io.sapl.server.ce.security;

import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import java.util.List;
import java.util.Properties;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import lombok.Setter;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Setter
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Caching.class, JCacheCacheManager.class })
@AutoConfigureBefore(AuthenticationCacheConfig.class)
public class RateLimiterCacheManagerConfig implements BeanClassLoaderAware {
    public static final String CACHE_PROVIDER_FQN = CaffeineCachingProvider.class.getName();

    private ClassLoader beanClassLoader;

    @Bean
    @Primary
    JCacheCacheManager cacheManager(CacheManager jCacheCacheManager) {
        return new JCacheCacheManager(jCacheCacheManager);
    }

    @Bean
    CacheManager jCacheCacheManager(ObjectProvider<javax.cache.configuration.Configuration<?, ?>> defaultCacheConfiguration) {
        CacheManager jCacheCacheManager = createCacheManager();
        List<String> cacheNames = List.of("buckets");

        for (String cacheName : cacheNames) {
            jCacheCacheManager.createCache(cacheName,
                defaultCacheConfiguration.getIfAvailable(MutableConfiguration::new));
        }

        return jCacheCacheManager;
    }

    private CacheManager createCacheManager() {
        CachingProvider cachingProvider = getCachingProvider();
        return cachingProvider.getCacheManager(null, this.beanClassLoader, new Properties());
    }

    private CachingProvider getCachingProvider() {
        return Caching.getCachingProvider(RateLimiterCacheManagerConfig.CACHE_PROVIDER_FQN);
    }

}
