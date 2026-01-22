package com.example.RateLimiting.repository;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Redis implementation of TokenBucketRepository.
 * 
 * <p>This class handles all Redis operations for storing and retrieving
 * token bucket state. It follows Repository Pattern and Single Responsibility
 * Principle by focusing solely on data persistence.</p>
 * 
 * <p><b>Storage Format:</b></p>
 * <ul>
 *   <li>Token count stored as string: "rate_limiter:tokens:{clientId}"</li>
 *   <li>Last refill time stored as string: "rate_limiter:last_refill:{clientId}"</li>
 * </ul>
 * 
 * <p><b>Note:</b> This class is not annotated with @Repository because it's
 * created manually by RateLimiterStrategyFactory. This allows proper dependency
 * injection of the capacity value.</p>
 */
public class RedisTokenBucketRepository implements TokenBucketRepository {
    
    private final JedisPool jedisPool;
    private final long defaultCapacity;
    
    /**
     * Creates a new Redis repository with default capacity for initial states.
     * 
     * @param jedisPool Jedis connection pool
     * @param defaultCapacity default capacity for new token buckets
     */
    public RedisTokenBucketRepository(JedisPool jedisPool, long defaultCapacity) {
        this.jedisPool = jedisPool;
        this.defaultCapacity = defaultCapacity;
    }
    
    @Override
    public TokenBucketState getState(String clientId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String tokenKey = RedisKeyGenerator.getTokenKey(clientId);
            String lastRefillKey = RedisKeyGenerator.getLastRefillKey(clientId);
            
            String tokenCountStr = jedis.get(tokenKey);
            String lastRefillStr = jedis.get(lastRefillKey);
            
            long currentTime = System.currentTimeMillis();
            
            long tokenCount = tokenCountStr != null 
                ? Long.parseLong(tokenCountStr) 
                : defaultCapacity;
            
            long lastRefillTime = lastRefillStr != null 
                ? Long.parseLong(lastRefillStr) 
                : currentTime;
            
            return new TokenBucketState(tokenCount, lastRefillTime);
        }
    }
    
    @Override
    public void saveState(String clientId, TokenBucketState state) {
        try (Jedis jedis = jedisPool.getResource()) {
            String tokenKey = RedisKeyGenerator.getTokenKey(clientId);
            String lastRefillKey = RedisKeyGenerator.getLastRefillKey(clientId);
            
            jedis.set(tokenKey, String.valueOf(state.getTokenCount()));
            jedis.set(lastRefillKey, String.valueOf(state.getLastRefillTime()));
        }
    }
}
