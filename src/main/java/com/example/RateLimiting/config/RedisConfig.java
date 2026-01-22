package com.example.RateLimiting.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Configuration class for Redis connection pool.
 * 
 * <p>This class is responsible for creating and configuring the Jedis connection pool.
 * Separated from RedisProperties to follow Single Responsibility Principle.</p>
 * 
 * <p><b>Connection Pool Settings:</b></p>
 * <ul>
 *   <li>MaxTotal: 50 - Maximum connections in pool</li>
 *   <li>MaxIdle: 10 - Maximum idle connections</li>
 *   <li>MinIdle: 5 - Minimum idle connections</li>
 *   <li>TestOnBorrow: true - Test connection before using</li>
 *   <li>TestOnReturn: true - Test connection when returning</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    
    private final RedisProperties redisProperties;
    
    /**
     * Creates and configures the Jedis connection pool.
     * 
     * <p>JedisPool maintains a pool of pre-established Redis connections that can be
     * reused, improving performance by avoiding connection overhead.</p>
     * 
     * @return configured JedisPool instance
     */
    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(50);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(5);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        
        return new JedisPool(
            poolConfig,
            redisProperties.getHost(),
            redisProperties.getPort(),
            redisProperties.getTimeout()
        );
    }
}
