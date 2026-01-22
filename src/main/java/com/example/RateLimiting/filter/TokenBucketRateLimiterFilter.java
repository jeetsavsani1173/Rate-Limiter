package com.example.RateLimiting.filter;

import com.example.RateLimiting.extractor.ClientIdentifierExtractor;
import com.example.RateLimiting.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Gateway filter for rate limiting using token bucket algorithm.
 * 
 * <p>This filter intercepts requests before they reach backend services and
 * applies rate limiting. It follows Single Responsibility Principle by
 * focusing solely on HTTP request/response handling and delegating rate
 * limiting logic to RateLimiterService.</p>
 * 
 * <p><b>Flow:</b></p>
 * <ol>
 *   <li>Extract client ID from request</li>
 *   <li>Check if request is allowed (rate limit check)</li>
 *   <li>If allowed: forward request and add rate limit headers</li>
 *   <li>If not allowed: return HTTP 429 with error message</li>
 * </ol>
 */
@Component
public class TokenBucketRateLimiterFilter extends AbstractGatewayFilterFactory<TokenBucketRateLimiterFilter.Config> {
    
    private final RateLimiterService rateLimiterService;
    private final ClientIdentifierExtractor clientIdentifierExtractor;
    
    /**
     * Configuration class for the filter (currently empty, can be extended).
     */
    public static class Config {
        // Can add filter-specific configuration here if needed
    }
    
    public TokenBucketRateLimiterFilter(
            RateLimiterService rateLimiterService,
            ClientIdentifierExtractor clientIdentifierExtractor) {
        super(Config.class);
        this.rateLimiterService = rateLimiterService;
        this.clientIdentifierExtractor = clientIdentifierExtractor;
    }
    
    @Override
    public Config newConfig() {
        return new Config();
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            
            // Extract client identifier
            String clientId = clientIdentifierExtractor.extractClientId(request);
            
            // Check rate limit
            if (!rateLimiterService.isRequestAllowed(clientId)) {
                // Rate limit exceeded - return 429
                return handleRateLimitExceeded(response, clientId);
            }
            
            // Request allowed - forward to next filter/backend
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                addRateLimitHeaders(response, clientId);
            }));
        };
    }
    
    /**
     * Handles rate limit exceeded scenario.
     * 
     * @param response HTTP response
     * @param clientId client identifier
     * @return Mono with error response
     */
    private Mono<Void> handleRateLimitExceeded(ServerHttpResponse response, String clientId) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        addRateLimitHeaders(response, clientId);
        
        String errorBody = String.format(
            "{\"error\":\"Rate limit exceeded\",\"clientId\":\"%s\"}",
            clientId
        );
        
        return response.writeWith(
            Mono.just(response.bufferFactory().wrap(errorBody.getBytes(StandardCharsets.UTF_8)))
        );
    }
    
    /**
     * Adds rate limit headers to the HTTP response.
     * 
     * @param response HTTP response
     * @param clientId client identifier
     */
    private void addRateLimitHeaders(ServerHttpResponse response, String clientId) {
        response.getHeaders().add("X-RateLimit-Limit",
            String.valueOf(rateLimiterService.getCapacity()));
        response.getHeaders().add("X-RateLimit-Remaining",
            String.valueOf(rateLimiterService.getRemainingTokens(clientId)));
    }
}
