package com.example.RateLimiting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Controller for user-related endpoints.
 * This endpoint is accessible via /api/users and goes through the rate limiter filter.
 * The gateway strips the /api prefix, so this controller handles /users.
 * Note: Uses reactive types (Mono) to work with Spring Cloud Gateway.
 */
@RestController
@RequestMapping("/users")
public class UsersController {

    /**
     * Get list of users.
     * This endpoint is rate-limited when accessed via /api/users
     * Each request will consume one token from the rate limiter.
     * 
     * @return List of sample users wrapped in Mono
     */
    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> getUsers() {
        List<Map<String, String>> users = List.of(
            Map.of("id", "1", "name", "John Doe", "email", "john.doe@example.com"),
            Map.of("id", "2", "name", "Jane Smith", "email", "jane.smith@example.com"),
            Map.of("id", "3", "name", "Bob Johnson", "email", "bob.johnson@example.com"),
            Map.of("id", "4", "name", "Alice Williams", "email", "alice.williams@example.com"),
            Map.of("id", "5", "name", "Charlie Brown", "email", "charlie.brown@example.com")
        );

        Map<String, Object> response = Map.of(
            "users", users,
            "total", users.size(),
            "message", "Users retrieved successfully"
        );

        return Mono.just(ResponseEntity.ok(response));
    }
}
