package com.example.RateLimiting.repository;

/**
 * Repository interface for token bucket state persistence.
 * 
 * <p>This interface abstracts data access operations for token bucket state.
 * Follows Repository Pattern, allowing different storage implementations
 * (Redis, Database, In-Memory) without changing business logic.</p>
 * 
 * <p><b>Benefits:</b></p>
 * <ul>
 *   <li>Decouples business logic from storage implementation</li>
 *   <li>Easy to test by mocking this interface</li>
 *   <li>Can switch storage backends without code changes</li>
 * </ul>
 */
public interface TokenBucketRepository {
    
    /**
     * Retrieves the current token bucket state for a client.
     * 
     * @param clientId unique identifier for the client
     * @return TokenBucketState containing current token count and last refill time
     */
    TokenBucketState getState(String clientId);
    
    /**
     * Saves the token bucket state for a client.
     * 
     * @param clientId unique identifier for the client
     * @param state the token bucket state to save
     */
    void saveState(String clientId, TokenBucketState state);
}
