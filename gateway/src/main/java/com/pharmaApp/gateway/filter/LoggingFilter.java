package com.pharmacare.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@Order(-3) // S'exécute EN PREMIER dans la chaîne
public class LoggingFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        // Injecte X-Correlation-Id dans la requête vers les services downstream
        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header("X-Correlation-Id", correlationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        log.info("[{}] → {} {}", correlationId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath());

        // then() s'exécute après que la réponse est revenue
        return chain.filter(mutatedExchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value() : 0;

            log.info("[{}] ← {} {} {}ms",
                    correlationId,
                    statusCode,
                    exchange.getRequest().getURI().getPath(),
                    duration);
        }));
    }
}