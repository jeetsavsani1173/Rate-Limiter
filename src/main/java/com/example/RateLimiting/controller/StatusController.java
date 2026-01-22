package com.example.RateLimiting.controller;

import com.example.RateLimiting.extractor.ClientIdentifierExtractor;
import com.example.RateLimiting.service.RateLimiterService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controller for health check and rate limit status endpoints.
 * These endpoints are not routed through the gateway filter.
 * Note: Spring Cloud Gateway is reactive, so this uses reactive types.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/gateway")
public class StatusController {

    private final RateLimiterService rateLimiterService;
    private final ClientIdentifierExtractor clientIdentifierExtractor;

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, String>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Rate Limiter Gateway"
        )));
    }

    /**
     * Endpoint to check rate limit status for a client.
     */
    @GetMapping("/rate-limit/status")
    public Mono<ResponseEntity<Map<String, Object>>> getRateLimitStatus(ServerWebExchange exchange) {
        String clientId = clientIdentifierExtractor.extractClientId(exchange.getRequest());
        return Mono.just(ResponseEntity.ok(Map.of(
                "clientId", clientId,
                "capacity", rateLimiterService.getCapacity(),
                "availableTokens", rateLimiterService.getRemainingTokens(clientId)
        )));
    }
}

