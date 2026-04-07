package com.pharmaApp.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(-2) // Après LoggingFilter(-3), avant RateLimitingFilter(-1)
public class UserContextFilter implements GlobalFilter {  // ✅ GlobalFilter, pas GatewayFilter

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Jwt.class)
                .flatMap(jwt -> {
                    String userId = jwt.getSubject();
                    String email  = jwt.getClaimAsString("email");
                    String role   = extractRole(jwt);

                    log.debug("UserContext → userId={} role={}", userId, role);

                    var mutatedRequest = exchange.getRequest()
                            .mutate()
                            .header("X-User-Id",    userId != null ? userId : "")
                            .header("X-User-Email", email  != null ? email  : "")
                            .header("X-User-Role",  role)
                            .build();

                    return chain.filter(
                            exchange.mutate().request(mutatedRequest).build()
                    );
                })
                // Route publique sans JWT — on continue sans header
                .switchIfEmpty(chain.filter(exchange));
    }

    private String extractRole(Jwt jwt) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return "UNKNOWN";

        @SuppressWarnings("unchecked")
        var roles = (java.util.List<String>) realmAccess.get("roles");
        if (roles == null) return "UNKNOWN";

        return roles.stream()
                .filter(r -> r.equals("PHARMACIEN")
                        || r.equals("PATIENT")
                        || r.equals("PROCHE"))
                .findFirst()
                .orElse("UNKNOWN");
    }
}