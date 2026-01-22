package com.example.RateLimiting.config;

import lombok.AllArgsConstructor;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Configuration;
import com.example.RateLimiting.filter.TokenBucketRateLimiterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.gateway.route.RouteLocator;

/**
 * Configuration class for Spring Cloud Gateway routing.
 * 
 * <p>This class defines how incoming requests are routed through the gateway
 * and which filters are applied to specific routes. It configures the gateway
 * to intercept requests matching certain patterns and apply rate limiting
 * before forwarding them to backend services.</p>
 * 
 * <p><b>SOLID Principles Applied:</b></p>
 * <ul>
 *   <li><b>Single Responsibility:</b> This class is solely responsible for
 *       configuring gateway routes. It doesn't handle rate limiting logic,
 *       which is delegated to the filter.</li>
 *   <li><b>Dependency Inversion:</b> Depends on abstractions (RateLimiterProperties,
 *       TokenBucketRateLimiterFilter) rather than concrete implementations.</li>
 * </ul>
 * 
 * <p><b>Routing Flow:</b></p>
 * <ol>
 *   <li>Request arrives at gateway (e.g., /api/users)</li>
 *   <li>Gateway matches the route pattern (/api/**)</li>
 *   <li>Rate limiter filter is applied (checks and consumes tokens)</li>
 *   <li>Prefix is stripped (/api/users → /users)</li>
 *   <li>Request is forwarded to backend service</li>
 * </ol>
 * 
 * <p><b>Current Configuration:</b></p>
 * <ul>
 *   <li><b>Route Pattern:</b> /api/** (all requests starting with /api)</li>
 *   <li><b>Filter:</b> TokenBucketRateLimiterFilter (applies rate limiting)</li>
 *   <li><b>Strip Prefix:</b> 1 (removes /api from the path)</li>
 *   <li><b>Backend URL:</b> Configured in application.properties</li>
 * </ul>
 * 
 * <p><b>Example:</b></p>
 * <pre>
 * Request: GET /api/users
 * After stripPrefix: GET /users
 * Forwarded to: http://localhost:8080/users
 * </pre>
 * 
 * @author Rate Limiting Team
 * @see TokenBucketRateLimiterFilter
 * @see RateLimiterProperties
 */
@Configuration
@AllArgsConstructor
public class GatewayConfig {

    private final RateLimiterProperties rateLimiterProperties;
    private final TokenBucketRateLimiterFilter tokenBucketRateLimiterFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder){
        return builder.routes()
                .route("api-route", r -> r
                        .path("/api/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(tokenBucketRateLimiterFilter.apply(new TokenBucketRateLimiterFilter.Config()))
                        )
                        .uri(rateLimiterProperties.getApiServerUrl()))
                .build();
    }
}

// Request arrives > stripPrefix(1) > remove /api/prefix > TokenBucketRateLimiterFilter > api-server-url