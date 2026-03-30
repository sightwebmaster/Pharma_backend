/*
package com.pharmaApp.treatement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()  // 👈 TOUT PERMETTRE TEMPORAIREMENT
                );
        return http.build();
    }
}


 */


package com.pharmaApp.treatement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("dev")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()  // ← Garde l'authentification
                )
                .httpBasic(httpBasic -> httpBasic.realmName("Treatment API"));  // ← Authentification basique

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails pharmacien = User.withUsername("pharmacien456")
                .password("{noop}secret")  // {noop} = pas d'encodage
                .roles("PHARMACIEN")
                .build();

        UserDetails patient = User.withUsername("patient123")
                .password("{noop}secret")
                .roles("PATIENT")
                .build();

        return new InMemoryUserDetailsManager(pharmacien, patient);
    }
}