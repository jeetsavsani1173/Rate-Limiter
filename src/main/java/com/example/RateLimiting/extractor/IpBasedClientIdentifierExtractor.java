package com.example.RateLimiting.extractor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/**
 * IP-based client identifier extractor implementation.
 * 
 * <p>This implementation extracts client identifiers based on IP addresses.
 * It checks the X-Forwarded-For header first (for requests behind proxies),
 * then falls back to the direct connection IP address.</p>
 * 
 * <p><b>Priority:</b></p>
 * <ol>
 *   <li>X-Forwarded-For header (first IP if multiple)</li>
 *   <li>Direct connection remote address</li>
 *   <li>"unknown" as fallback</li>
 * </ol>
 */
@Slf4j
@Component
public class IpBasedClientIdentifierExtractor implements ClientIdentifierExtractor {
    
    @Override
    public String extractClientId(ServerHttpRequest request) {
        // Check X-Forwarded-For header first (for requests behind proxy/load balancer)
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // If multiple IPs, use the first one (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Fallback to direct connection IP
        var remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        
        // Default fallback
        log.warn("Unable to extract client ID from request, using 'unknown'");
        return "unknown";
    }
}
