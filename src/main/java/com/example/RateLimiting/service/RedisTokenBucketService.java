package com.example.RateLimiting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.RateLimiting.config.RateLimiterProperties;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

// store token bucket data in redis
// manage token consumption and refilling per client
// provide methods to check token availability and update token counts with rate limiting core logic
@Service
@RequiredArgsConstructor
public class RedisTokenBucketService {

    private final JedisPool jedisPool;
    private final RateLimiterProperties rateLimiterProperties;

    private final String TOKEN_KEY_PREFIX = "rate_limiter:tokens:";
    private static final String LAST_REFILL_KEY_PREFIX = "rate_limiter:last_refill:";

    // Pattern
    // rate_limiter:tokens:{clientId}

    // Ex1: rate_limiter:tokens:192.185.2.78 = '7' > current token count
    // Ex2: rate_limiter:last_refill:192.185.2.78 = 233554633 > timestamp of last refill

    public boolean isAllowed(String clientId) {
        String tokenKey = TOKEN_KEY_PREFIX + clientId;
        String lastRefillKey = LAST_REFILL_KEY_PREFIX + clientId;

        try (var jedis = jedisPool.getResource()) {
            long currentTime = System.currentTimeMillis();

            // Get current token count and last refill time atomically
            String tokenCountStr = jedis.get(tokenKey);
            String lastRefillStr = jedis.get(lastRefillKey);

            long tokenCount = tokenCountStr != null ? Long.parseLong(tokenCountStr) : rateLimiterProperties.getCapacity();
            long lastRefillTime = lastRefillStr != null ? Long.parseLong(lastRefillStr) : currentTime;

            // Calculate tokens to add based on elapsed time
            long elapsedTime = currentTime - lastRefillTime;
            int tokensToAdd = (int) (elapsedTime / rateLimiterProperties.getRefillInterval()) * (int) rateLimiterProperties.getRefillRate();

            // Update token count (refill)
            tokenCount = Math.min(rateLimiterProperties.getCapacity(), tokenCount + tokensToAdd);
            lastRefillTime = currentTime - (elapsedTime % rateLimiterProperties.getRefillInterval());

            if (tokenCount > 0) {
                // Consume a token and save atomically
                tokenCount--;
                jedis.set(tokenKey, String.valueOf(tokenCount));
                jedis.set(lastRefillKey, String.valueOf(lastRefillTime));
                return true;
            } else {
                // No tokens available, but still update lastRefillTime
                jedis.set(tokenKey, String.valueOf(tokenCount));
                jedis.set(lastRefillKey, String.valueOf(lastRefillTime));
                return false;
            }
        }
    }

    public long getCapacity() {
        return rateLimiterProperties.getCapacity();
    }

    public long getAvailableTokens(String clientId) {
        String tokenKey = TOKEN_KEY_PREFIX + clientId;

        try(Jedis jedis = jedisPool.getResource()) {
            // Refill tokens without consuming
            refillTokens(clientId, jedis);
            // Get the updated token count
            String tokenStr = jedis.get(tokenKey);
            return tokenStr != null ? Long.parseLong(tokenStr) : rateLimiterProperties.getCapacity();
        }
    }

    private void refillTokens(String clientId, Jedis jedis) {
        String tokenKey = TOKEN_KEY_PREFIX + clientId;
        String lastRefillKey = LAST_REFILL_KEY_PREFIX + clientId;

        long currentTime = System.currentTimeMillis();

        // Get current token count and last refill time
        String tokenCountStr = jedis.get(tokenKey);
        String lastRefillStr = jedis.get(lastRefillKey);

        long tokenCount = tokenCountStr != null ? Long.parseLong(tokenCountStr) : rateLimiterProperties.getCapacity();
        long lastRefillTime = lastRefillStr != null ? Long.parseLong(lastRefillStr) : currentTime;

        // Calculate tokens to add based on elapsed time
        long elapsedTime = currentTime - lastRefillTime;
        int tokensToAdd = (int) (elapsedTime / rateLimiterProperties.getRefillInterval()) * (int) rateLimiterProperties.getRefillRate();

        // Update token count
        tokenCount = Math.min(rateLimiterProperties.getCapacity(), tokenCount + tokensToAdd);
        lastRefillTime = currentTime - (elapsedTime % rateLimiterProperties.getRefillInterval());

        // Save updated values to Redis
        jedis.set(tokenKey, String.valueOf(tokenCount));
        jedis.set(lastRefillKey, String.valueOf(lastRefillTime));
    }
}
