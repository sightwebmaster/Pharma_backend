package com.pharmaApp.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
public class RateLimitingFilter implements GatewayFilter {

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

        // Si pas de userId (ne devrait pas arriver après SecurityConfig)
        if (userId == null) {
            return chain.filter(exchange);
        }

        String redisKey = "rate_limit:" + userId;

        return redisTemplate.opsForValue()
                .increment(redisKey)
                .flatMap(count -> {
                    if (count == 1) {
                        // Première requête de la fenêtre → on pose l'expiration
                        return redisTemplate.expire(redisKey, Duration.ofSeconds(WINDOW_SECONDS))
                                .then(chain.filter(exchange));
                    }
                    if (count > MAX_REQUESTS) {
                        log.warn("Rate limit dépassé pour userId={} count={}", userId, count);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders()
                                .add("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
                        exchange.getResponse().getHeaders()
                                .add("X-RateLimit-Remaining", "0");
                        return exchange.getResponse().setComplete();
                    }
                    // Dans la fenêtre, sous la limite
                    exchange.getResponse().getHeaders()
                            .add("X-RateLimit-Remaining",
                                    String.valueOf(MAX_REQUESTS - count));
                    return chain.filter(exchange);
                });
    }
}