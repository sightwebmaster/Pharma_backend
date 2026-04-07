package com.pharmaApp.adherence.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig — adherence-service
 *
 * Ce service est derrière le Gateway.
 * Le Gateway valide le JWT et propage X-User-Id / X-User-Role.
 * Ce service fait confiance aux headers — il ne valide pas le JWT.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/actuator/health").permitAll()
                        .anyRequest().permitAll() // Sécurité gérée par le Gateway
                );
        return http.build();
    }
}