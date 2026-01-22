package com.example.RateLimiting.service;

import com.example.RateLimiting.strategy.RateLimiterStrategy;
import com.example.RateLimiting.strategy.RateLimiterStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service layer for rate limiting operations.
 * 
 * <p>This service acts as a facade for rate limiting operations, delegating
 * to the appropriate strategy. It follows Facade Pattern and provides a
 * clean API for rate limiting functionality.</p>
 * 
 * <p><b>Benefits:</b></p>
 * <ul>
 *   <li>Single entry point for rate limiting operations</li>
 *   <li>Can switch strategies without changing client code</li>
 *   <li>Follows Dependency Inversion Principle</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RateLimiterService {
    
    private final RateLimiterStrategyFactory strategyFactory;
    private RateLimiterStrategy strategy;
    
    /**
     * Gets or creates the rate limiter strategy (lazy initialization).
     * 
     * @return RateLimiterStrategy instance
     */
    private RateLimiterStrategy getStrategy() {
        if (strategy == null) {
            strategy = strategyFactory.createDefaultStrategy();
        }
        return strategy;
    }
    
    /**
     * Checks if a request from the given client is allowed.
     * 
     * @param clientId unique identifier for the client
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isRequestAllowed(String clientId) {
        return getStrategy().isAllowed(clientId);
    }
    
    /**
     * Gets the number of remaining tokens for a client.
     * 
     * @param clientId unique identifier for the client
     * @return number of remaining tokens
     */
    public long getRemainingTokens(String clientId) {
        return getStrategy().getAvailableTokens(clientId);
    }
    
    /**
     * Gets the maximum capacity of the rate limiter.
     * 
     * @return maximum capacity
     */
    public long getCapacity() {
        return getStrategy().getCapacity();
    }
}
