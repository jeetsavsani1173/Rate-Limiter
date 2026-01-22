package com.example.RateLimiting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimiterService {
    private final RedisTokenBucketService redisTokenBucketService;

    public boolean isRequestAllowed(String clientId) {
        return redisTokenBucketService.isAllowed(clientId);
    }

    public long getRemainingTokens(String clientId) {
        return redisTokenBucketService.getAvailableTokens(clientId);
    }

    public long getCapacity() {
        return redisTokenBucketService.getCapacity();
    }
}
