package com.example.RateLimiting.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value object representing the state of a token bucket for a client.
 * 
 * <p>This immutable value object encapsulates all the data needed to represent
 * a token bucket's current state. It follows Value Object pattern from DDD.</p>
 * 
 * <p><b>Fields:</b></p>
 * <ul>
 *   <li><b>tokenCount:</b> Current number of tokens in the bucket</li>
 *   <li><b>lastRefillTime:</b> Timestamp (milliseconds) of the last token refill</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenBucketState {
    
    /**
     * Current number of tokens available in the bucket.
     */
    private long tokenCount;
    
    /**
     * Timestamp in milliseconds when tokens were last refilled.
     */
    private long lastRefillTime;
    
    /**
     * Checks if the bucket has any tokens available.
     * 
     * @return true if tokenCount > 0, false otherwise
     */
    public boolean hasTokens() {
        return tokenCount > 0;
    }
    
    /**
     * Creates a new state with default values (full capacity).
     * 
     * @param capacity maximum capacity of the bucket
     * @param currentTime current timestamp in milliseconds
     * @return new TokenBucketState with full capacity
     */
    public static TokenBucketState createInitialState(long capacity, long currentTime) {
        return new TokenBucketState(capacity, currentTime);
    }
}
