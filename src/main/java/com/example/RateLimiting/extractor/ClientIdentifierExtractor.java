package com.example.RateLimiting.extractor;

import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * Interface for extracting client identifiers from HTTP requests.
 * 
 * <p>This interface abstracts the logic of identifying clients for rate limiting.
 * Different implementations can extract client IDs based on IP address, API keys,
 * user IDs, or other criteria.</p>
 * 
 * <p><b>Current Implementation:</b> IP-based extraction</p>
 * <p><b>Future Extensions:</b> API key, JWT token, user ID based extraction</p>
 */
public interface ClientIdentifierExtractor {
    
    /**
     * Extracts a unique client identifier from the HTTP request.
     * 
     * <p>The identifier is used as a key for rate limiting, so all requests
     * from the same client should return the same identifier.</p>
     * 
     * @param request the HTTP request
     * @return unique client identifier (e.g., IP address, API key, user ID)
     */
    String extractClientId(ServerHttpRequest request);
}
