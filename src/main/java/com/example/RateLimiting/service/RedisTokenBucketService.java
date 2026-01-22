package com.example.RateLimiting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.RateLimiting.config.RateLimiterProperties;

import redis.clients.jedis.JedisPool;

// store token bucket data in redis
// manage token consumption and refilling per client
// provide methods to check token availability and update token counts with rate limiting core logic
@Service
@RequiredArgsConstructor
public class RedisTokenBucketService {

    private final JedisPool jedisPool;
    private final RateLimiterProperties rateLimiterProperties;

}
