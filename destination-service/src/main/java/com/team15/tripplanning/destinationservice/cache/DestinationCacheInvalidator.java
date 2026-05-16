package com.team15.tripplanning.destinationservice.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DestinationCacheInvalidator {

    private final RedisTemplate<String, Object> redisTemplate;

    public DestinationCacheInvalidator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void invalidateRevenueCache(Long destinationId) {
        // S2-F3 cache keys pattern: destination-service::S2-F3::<id>::*
        Set<String> keys = redisTemplate.keys("destination-service::S2-F3::" + destinationId + "::*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void invalidateDashboardCache(Long destinationId) {
        // S2-F12 cache key is simple: destination-service::S2-F12::<id>
        redisTemplate.delete("destination-service::S2-F12::" + destinationId);
    }
}