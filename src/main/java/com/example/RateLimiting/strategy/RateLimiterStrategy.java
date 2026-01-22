package com.example.RateLimiting.strategy;

/**
 * Strategy interface for rate limiting algorithms.
 * 
 * <p>This interface allows different rate limiting algorithms (Token Bucket,
 * Sliding Window, Fixed Window) to be implemented and used interchangeably.
 * Follows Strategy Pattern and Open/Closed Principle.</p>
 * 
 * <p><b>Usage:</b></p>
 * <pre>
 * RateLimiterStrategy strategy = factory.createStrategy("token-bucket");
 * boolean allowed = strategy.isAllowed("client-123");
 * </pre>
 */
public interface RateLimiterStrategy {
    
    /**
     * Checks if a request from the given client is allowed based on rate limiting rules.
     * 
     * @param clientId unique identifier for the client (e.g., IP address)
     * @return true if request is allowed, false if rate limit exceeded
     */
    boolean isAllowed(String clientId);
    
    /**
     * Gets the number of available tokens for the given client.
     * 
     * @param clientId unique identifier for the client
     * @return number of available tokens
     */
    long getAvailableTokens(String clientId);
    
    /**
     * Gets the maximum capacity (total tokens) for the rate limiter.
     * 
     * @return maximum capacity
     */
    long getCapacity();
}
