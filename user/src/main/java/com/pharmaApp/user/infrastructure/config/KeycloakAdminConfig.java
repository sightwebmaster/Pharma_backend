package com.pharmaApp.user.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class KeycloakAdminConfig {

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    @PostConstruct
    public void logConfig() {
        System.out.println("=== KEYCLOAK CONFIG ===");
        System.out.println("server-url: " + serverUrl);
        System.out.println("realm: " + realm);
        System.out.println("client-id: " + clientId);
        System.out.println("client-secret: " + clientSecret.substring(0, 5) + "...");
        System.out.println("======================");
    }

    // ❌ pas de @Bean RestTemplate ici
}