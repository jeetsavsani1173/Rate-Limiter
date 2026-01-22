package com.example.RateLimiting.repository;

/**
 * Utility class for generating Redis keys for token bucket storage.
 * 
 * <p>This class centralizes key generation logic, following Single Responsibility
 * Principle. All Redis keys used for rate limiting are generated here.</p>
 * 
 * <p><b>Key Patterns:</b></p>
 * <ul>
 *   <li>Token count: "rate_limiter:tokens:{clientId}"</li>
 *   <li>Last refill: "rate_limiter:last_refill:{clientId}"</li>
 * </ul>
 */
public class RedisKeyGenerator {
    
    private static final String TOKEN_KEY_PREFIX = "rate_limiter:tokens:";
    private static final String LAST_REFILL_KEY_PREFIX = "rate_limiter:last_refill:";
    
    /**
     * Generates the Redis key for storing token count.
     * 
     * @param clientId unique client identifier
     * @return Redis key for token count
     */
    public static String getTokenKey(String clientId) {
        return TOKEN_KEY_PREFIX + clientId;
    }
    
    /**
     * Generates the Redis key for storing last refill timestamp.
     * 
     * @param clientId unique client identifier
     * @return Redis key for last refill time
     */
    public static String getLastRefillKey(String clientId) {
        return LAST_REFILL_KEY_PREFIX + clientId;
    }
}
