package com.mahdi.url_shortener.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisUrlCache {
    

    private final StringRedisTemplate redisTemplate;

    public RedisUrlCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(
        String shortCode,
        String originalUrl,
        Duration ttl
    ) {
        redisTemplate.opsForValue().set(
            buildKey(shortCode),
            originalUrl,
            ttl
        );
    }

    public String get(String shortCode) {
        return redisTemplate.opsForValue().get(
            buildKey(shortCode)
        );
    }

    private String buildKey(String shortCode) {
        return "url:" + shortCode;
    }
}
