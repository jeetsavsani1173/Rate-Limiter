package com.example.RateLimiting.algorithm;

import com.example.RateLimiting.repository.TokenBucketState;

/**
 * Pure token bucket algorithm implementation.
 * 
 * <p>This class contains the core business logic for the token bucket algorithm
 * without any storage concerns. It follows Single Responsibility Principle by
 * focusing solely on algorithm calculations.</p>
 * 
 * <p><b>Token Bucket Algorithm:</b></p>
 * <ol>
 *   <li>Tokens are added to the bucket at a fixed rate (refillRate per refillInterval)</li>
 *   <li>Each request consumes one token</li>
 *   <li>If bucket is empty, request is rejected</li>
 *   <li>Bucket capacity limits maximum tokens</li>
 * </ol>
 */
public class TokenBucketAlgorithm {
    
    private final long capacity;
    private final long refillRate;
    private final long refillInterval;
    
    /**
     * Creates a new instance with the specified configuration.
     * 
     * @param capacity maximum number of tokens the bucket can hold
     * @param refillRate number of tokens to add per refill interval
     * @param refillInterval time interval in milliseconds between refills
     */
    public TokenBucketAlgorithm(long capacity, long refillRate, long refillInterval) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.refillInterval = refillInterval;
    }
    
    /**
     * Refills tokens based on elapsed time and returns updated state.
     * 
     * @param currentState current token bucket state
     * @param currentTime current timestamp in milliseconds
     * @return new state with refilled tokens
     */
    public TokenBucketState refillTokens(TokenBucketState currentState, long currentTime) {
        long elapsedTime = currentTime - currentState.getLastRefillTime();
        
        // Calculate how many complete refill intervals have passed
        long intervalsPassed = elapsedTime / refillInterval;
        
        // Calculate tokens to add
        long tokensToAdd = intervalsPassed * refillRate;
        
        // Update token count (cap at capacity)
        long newTokenCount = Math.min(capacity, currentState.getTokenCount() + tokensToAdd);
        
        // Update last refill time to the most recent refill boundary
        long newLastRefillTime = currentState.getLastRefillTime() + (intervalsPassed * refillInterval);
        
        return new TokenBucketState(newTokenCount, newLastRefillTime);
    }
    
    /**
     * Attempts to consume one token from the bucket.
     * 
     * @param state current token bucket state
     * @return new state after consuming a token, or null if no tokens available
     */
    public TokenBucketState consumeToken(TokenBucketState state) {
        if (!state.hasTokens()) {
            return null;
        }
        
        return new TokenBucketState(state.getTokenCount() - 1, state.getLastRefillTime());
    }
    
    /**
     * Gets the maximum capacity of the token bucket.
     * 
     * @return capacity
     */
    public long getCapacity() {
        return capacity;
    }
}
