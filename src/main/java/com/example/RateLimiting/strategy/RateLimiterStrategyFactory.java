package com.example.RateLimiting.strategy;

import com.example.RateLimiting.algorithm.TokenBucketAlgorithm;
import com.example.RateLimiting.config.RateLimiterProperties;
import com.example.RateLimiting.repository.RedisTokenBucketRepository;
import com.example.RateLimiting.repository.TokenBucketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPool;

/**
 * Factory for creating rate limiter strategies.
 * 
 * <p>This factory creates appropriate rate limiter strategy instances based on
 * configuration. Follows Factory Pattern and allows easy extension for new
 * algorithm types.</p>
 * 
 * <p><b>Supported Strategies:</b></p>
 * <ul>
 *   <li>token-bucket (default)</li>
 * </ul>
 * 
 * <p><b>Future Extensions:</b></p>
 * <ul>
 *   <li>sliding-window</li>
 *   <li>fixed-window</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RateLimiterStrategyFactory {
    
    private final RateLimiterProperties properties;
    private final JedisPool jedisPool;
    
    /**
     * Creates a rate limiter strategy based on the specified type.
     * 
     * @param strategyType type of strategy to create (e.g., "token-bucket")
     * @return RateLimiterStrategy instance
     * @throws IllegalArgumentException if strategy type is not supported
     */
    public RateLimiterStrategy createStrategy(String strategyType) {
        return switch (strategyType.toLowerCase()) {
            case "token-bucket", "tokenbucket" -> createTokenBucketStrategy();
            default -> throw new IllegalArgumentException(
                "Unsupported rate limiter strategy: " + strategyType
            );
        };
    }
    
    /**
     * Creates the default token bucket strategy.
     * 
     * @return TokenBucketStrategy instance
     */
    public RateLimiterStrategy createDefaultStrategy() {
        return createTokenBucketStrategy();
    }
    
    private RateLimiterStrategy createTokenBucketStrategy() {
        // Create algorithm with configuration
        TokenBucketAlgorithm algorithm = new TokenBucketAlgorithm(
            properties.getCapacity(),
            properties.getRefillRate(),
            properties.getRefillInterval()
        );
        
        // Create repository
        TokenBucketRepository repository = new RedisTokenBucketRepository(
            jedisPool,
            properties.getCapacity()
        );
        
        // Create and return strategy
        return new TokenBucketStrategy(algorithm, repository);
    }
}
