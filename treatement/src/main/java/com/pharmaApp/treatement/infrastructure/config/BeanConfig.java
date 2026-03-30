package com.pharmaApp.treatement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * BeanConfig — Spring Boot 3.x
 *
 * RestTemplateBuilder a été supprimé du package spring-boot en Spring Boot 3.2+.
 * On utilise directement RestTemplate avec SimpleClientHttpRequestFactory
 * pour configurer les timeouts.
 */
@Configuration
@EnableScheduling
public class BeanConfig {

    @Bean
    public RestTemplate restTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);  // 3 secondes
        factory.setReadTimeout(5000);     // 5 secondes
        return new RestTemplate(factory);
    }
}