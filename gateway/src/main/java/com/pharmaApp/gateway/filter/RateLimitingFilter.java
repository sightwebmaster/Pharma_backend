package com.pharmaApp.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@Order(-1) // S'exécute EN DERNIER — après UserContextFilter qui a injecté X-User-Id
public class RateLimitingFilter implements GlobalFilter {  // ✅ GlobalFilter

    private static final int MAX_REQUESTS   = 100;
    private static final int WINDOW_SECONDS = 60;

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RateLimitingFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        String userId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-User-Id");

        // Route publique sans userId → pas de rate limiting
        if (userId == null || userId.isBlank()) {
            return chain.filter(exchange);
        }

        String redisKey = "rate_limit:" + userId;

        return redisTemplate.opsForValue()
                .increment(redisKey)
                .flatMap(count -> {
                    if (count == 1) {
                        // Première requête → pose l'expiration de la fenêtre
                        return redisTemplate.expire(redisKey, Duration.ofSeconds(WINDOW_SECONDS))
                                .then(chain.filter(exchange));
                    }

                    if (count > MAX_REQUESTS) {
                        log.warn("Rate limit dépassé → userId={} count={}", userId, count);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders()
                                .add("X-RateLimit-Limit",     String.valueOf(MAX_REQUESTS));
                        exchange.getResponse().getHeaders()
                                .add("X-RateLimit-Remaining", "0");
                        exchange.getResponse().getHeaders()
                                .add("Retry-After",           String.valueOf(WINDOW_SECONDS));
                        return exchange.getResponse().setComplete();
                    }

                    // Sous la limite → continue et informe le client
                    long remaining = MAX_REQUESTS - count;
                    exchange.getResponse().getHeaders()
                            .add("X-RateLimit-Remaining", String.valueOf(remaining));
                    return chain.filter(exchange);
                });
    }
}