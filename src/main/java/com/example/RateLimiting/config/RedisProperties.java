package com.example.RateLimiting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration properties for Redis connection.
 * 
 * <p>This class holds Redis connection properties loaded from application.properties.
 * The JedisPool bean is created in a separate configuration class to follow
 * Single Responsibility Principle.</p>
 */
@Component
@Data
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {
    private String host = "localhost";
    private int port = 6379;
    private int timeout = 2000;
}
