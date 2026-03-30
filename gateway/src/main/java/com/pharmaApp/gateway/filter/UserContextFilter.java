package com.pharmaApp.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class UserContextFilter implements GatewayFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Jwt.class)
                .flatMap(jwt -> {
                    // Extrait les claims du JWT Keycloak
                    String userId = jwt.getSubject();        // claim "sub" = UUID Keycloak
                    String role   = extractRole(jwt);        // claim "realm_access.roles"

                    log.debug("UserContext → userId={} role={}", userId, role);

                    // Injecte les headers vers les services downstream
                    var mutatedRequest = exchange.getRequest()
                            .mutate()
                            .header("X-User-Id",   userId)
                            .header("X-User-Role", role)
                            .build();

                    return chain.filter(
                            exchange.mutate().request(mutatedRequest).build()
                    );
                });
    }

    /**
     * Extrait le premier rôle métier du JWT.
     * On ignore les rôles Keycloak système (offline_access, uma_authorization).
     */
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