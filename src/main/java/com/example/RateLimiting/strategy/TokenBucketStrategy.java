package com.example.RateLimiting.strategy;

import com.example.RateLimiting.algorithm.TokenBucketAlgorithm;
import com.example.RateLimiting.repository.TokenBucketRepository;
import com.example.RateLimiting.repository.TokenBucketState;

/**
 * Token Bucket algorithm implementation of RateLimiterStrategy.
 * 
 * <p>This class orchestrates the token bucket algorithm by combining:
 * <ul>
 *   <li>TokenBucketAlgorithm - pure algorithm logic</li>
 *   <li>TokenBucketRepository - data persistence</li>
 * </ul>
 * 
 * <p>It follows Strategy Pattern and Single Responsibility Principle by
 * focusing on coordinating the algorithm and repository.</p>
 * 
 * <p><b>Note:</b> This class is not annotated with @Component because it's
 * created manually by RateLimiterStrategyFactory. This allows proper dependency
 * injection of algorithm and repository instances.</p>
 */
public class TokenBucketStrategy implements RateLimiterStrategy {
    
    private final TokenBucketAlgorithm algorithm;
    private final TokenBucketRepository repository;
    
    /**
     * Creates a new TokenBucketStrategy with the given algorithm and repository.
     * 
     * @param algorithm the token bucket algorithm implementation
     * @param repository the repository for persisting token bucket state
     */
    public TokenBucketStrategy(TokenBucketAlgorithm algorithm, TokenBucketRepository repository) {
        this.algorithm = algorithm;
        this.repository = repository;
    }
    
    @Override
    public boolean isAllowed(String clientId) {
        long currentTime = System.currentTimeMillis();
        
        // Get current state from repository
        TokenBucketState currentState = repository.getState(clientId);
        
        // Refill tokens based on elapsed time
        TokenBucketState refilledState = algorithm.refillTokens(currentState, currentTime);
        
        // Try to consume a token
        TokenBucketState newState = algorithm.consumeToken(refilledState);
        
        if (newState == null) {
            // No tokens available - save refilled state (without consuming)
            repository.saveState(clientId, refilledState);
            return false;
        }
        
        // Token consumed - save updated state
        repository.saveState(clientId, newState);
        return true;
    }
    
    @Override
    public long getAvailableTokens(String clientId) {
        long currentTime = System.currentTimeMillis();
        
        // Get current state
        TokenBucketState currentState = repository.getState(clientId);
        
        // Refill tokens (without consuming)
        TokenBucketState refilledState = algorithm.refillTokens(currentState, currentTime);
        
        // Save refilled state
        repository.saveState(clientId, refilledState);
        
        return refilledState.getTokenCount();
    }
    
    @Override
    public long getCapacity() {
        return algorithm.getCapacity();
    }
}
