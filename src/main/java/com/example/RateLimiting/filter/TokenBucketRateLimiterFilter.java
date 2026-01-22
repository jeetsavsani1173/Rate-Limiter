package com.example.RateLimiting.filter;

//Client Request > GatewayFilter (intercepts request) > Check Rate Limit)

//Global Filters > applied to all routes
//Route Filters > applied to specific routes
//Custom Filters > your own implementation of filter

import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import com.example.RateLimiting.service.RateLimiterService;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;

@Component
public class TokenBucketRateLimiterFilter extends AbstractGatewayFilterFactory<TokenBucketRateLimiterFilter.Config> {
    private final RateLimiterService rateLimiterService;

    public static class Config{}

    public TokenBucketRateLimiterFilter(RateLimiterService rateLimiterService){
        super(Config.class);
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public TokenBucketRateLimiterFilter.Config newConfig(){
        return new Config();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            String clientId = getClientId(request);

            if(!rateLimiterService.isRequestAllowed(clientId)){

                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                addRateLimitHeaders(response, clientId);

                String errorBody = String.format(
                        "{\"error\":\"Rate limited exceeded\",\"clientId\":\"%s\"}",
                        clientId
                );

                return response.writeWith(
                        Mono.just(response.bufferFactory().wrap(errorBody.getBytes(StandardCharsets.UTF_8)))
                );
            }

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                addRateLimitHeaders(response, clientId);
            }));
        };
    }

    private void addRateLimitHeaders(ServerHttpResponse response, String clientId){
        response.getHeaders().add("X-RateLimit-Limit",
                String.valueOf(rateLimiterService.getCapacity()));
        response.getHeaders().add("X-RateLimit-Remaining",
                String.valueOf(rateLimiterService.getRemainingTokens(clientId)));
    }

    //
    //X-Forwarded-For: 192.168.1.1, 10.0.0.1 uses > 192.168.1.1
    private String getClientId(ServerHttpRequest request){
        String xForwardFor = request.getHeaders().getFirst("X-Forwarded-For");
        if(xForwardFor != null && !xForwardFor.isEmpty()){
            return xForwardFor.split(",")[0].trim();
        }

        //Fallback to direct connection IP
        var remoteAddress=  request.getRemoteAddress();
        if(remoteAddress != null && remoteAddress.getHostName() != null){
            return remoteAddress.getAddress().getHostAddress();
        }

        //Default fallbck
        return "unknown";
    }
}
