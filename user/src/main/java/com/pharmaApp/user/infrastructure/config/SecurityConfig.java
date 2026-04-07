package com.pharmaApp.user.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig — user-service
 *
 * IMPORTANT : Ce service est derrière le Gateway.
 * Le Gateway a déjà validé le JWT et propagé X-User-Id / X-User-Role.
 * Ce service NE valide PAS le JWT — il fait confiance aux headers du Gateway.
 *
 * En production, ajouter une whitelist d'IP pour n'accepter que le Gateway.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Actuator health accessible sans auth (pour le Gateway/Docker healthcheck)
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Tout le reste est accessible — la sécurité est gérée par le Gateway
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}