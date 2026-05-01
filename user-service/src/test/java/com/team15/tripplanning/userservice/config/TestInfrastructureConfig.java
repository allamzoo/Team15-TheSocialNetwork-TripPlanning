package com.team15.tripplanning.userservice.config;

import com.team15.tripplanning.userservice.repository.AuthEventRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

@TestConfiguration
public class TestInfrastructureConfig {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> redisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }

    @Bean
    @Primary
    public AuthEventRepository authEventRepository() {
        return Mockito.mock(AuthEventRepository.class);
    }
}
