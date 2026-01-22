package com.example.RateLimiting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration properties class for rate limiter settings.
 * 
 * <p>This class holds all the configurable parameters for the token bucket
 * rate limiting algorithm. These properties are loaded from application.properties
 * and can be customized based on your rate limiting requirements.</p>
 * 
 * <p><b>SOLID Principles Applied:</b></p>
 * <ul>
 *   <li><b>Single Responsibility:</b> This class is solely responsible for
 *       holding configuration properties. It doesn't contain business logic
 *       or data access code.</li>
 *   <li><b>Open/Closed:</b> Properties can be extended by adding new fields
 *       without modifying existing code.</li>
 * </ul>
 * 
 * <p><b>Token Bucket Algorithm:</b></p>
 * <p>The token bucket algorithm works like a bucket that holds tokens. Each request
 * consumes one token. When the bucket is empty, requests are rejected. Tokens are
 * refilled at a specified rate over time.</p>
 * 
 * <p><b>Configuration Properties (from application.properties):</b></p>
 * <ul>
 *   <li><b>capacity:</b> Maximum number of tokens the bucket can hold (default: 10)
 *       <br>This is the burst limit - how many requests can be made in quick succession</li>
 *   <li><b>refillRate:</b> Number of tokens to add per refill interval (default: 5)
 *       <br>This controls how fast tokens are replenished</li>
 *   <li><b>refillInterval:</b> Time interval in milliseconds between refills (default: 100000 = 100 seconds)
 *       <br>This determines how often tokens are added to the bucket</li>
 *   <li><b>apiServerUrl:</b> Backend API server URL where requests are forwarded (default: http://localhost:8080)</li>
 *   <li><b>timeout:</b> Request timeout in milliseconds (default: 5000)</li>
 * </ul>
 * 
 * <p><b>Example Calculation:</b></p>
 * <pre>
 * Capacity: 10 tokens
 * Refill Rate: 5 tokens
 * Refill Interval: 100 seconds (100000 ms)
 * 
 * This means:
 * - Client can make 10 requests immediately (burst)
 * - After 100 seconds, 5 tokens are added
 * - Maximum sustained rate: 5 tokens per 100 seconds = 0.05 requests/second
 * </pre>
 * 
 * <p><b>Usage:</b></p>
 * <p>These properties are automatically injected into services that need rate limiting
 * configuration. They can be changed in application.properties without code changes.</p>
 * 
 * @author Rate Limiting Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    private long capacity = 10;
    private long refillRate = 5; // per second
    private long refillInterval = 100000; // in milliseconds
    private String apiServerUrl = "http://localhost:8080";
    private long timeout = 5000;
}