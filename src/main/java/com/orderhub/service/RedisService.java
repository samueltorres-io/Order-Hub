package com.orderhub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public void save(String refreshToken, String userId) {
        redisTemplate.opsForValue().set(refreshToken, userId, 7, TimeUnit.DAYS);
    }

    public String get(String refreshToken) {
        return redisTemplate.opsForValue().get(refreshToken);
    }

    public void delete(String refreshToken) {
        redisTemplate.delete(refreshToken);
    }
}