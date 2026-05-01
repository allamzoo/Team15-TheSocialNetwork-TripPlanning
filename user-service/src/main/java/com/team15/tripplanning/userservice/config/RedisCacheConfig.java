package com.team15.tripplanning.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    private GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer());
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        RedisCacheConfiguration base = defaultConfig(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put("s1-f1-users",        defaultConfig(Duration.ofMinutes(5)));
        configs.put("s1-f3-trip-summary",  defaultConfig(Duration.ofMinutes(10)));
        configs.put("s1-f5-top-travelers", defaultConfig(Duration.ofMinutes(10)));
        configs.put("s1-f6-profile",       defaultConfig(Duration.ofMinutes(10)));
        configs.put("s1-f8-pref-search",   defaultConfig(Duration.ofMinutes(5)));
        configs.put("s1-f9-travel-style",  defaultConfig(Duration.ofMinutes(5)));
        configs.put("s1-f12-activity",     defaultConfig(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(configs)
                .build();
    }

    private RedisCacheConfiguration defaultConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer()));
    }
}
