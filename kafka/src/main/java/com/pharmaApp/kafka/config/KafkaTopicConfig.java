package com.pharmaApp.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // ── Constantes des noms de topics ───────────────────────────
    // Centraliser ici évite les fautes de frappe entre services
    public static final String TOPIC_PRISE_CONFIRMEE    = "pharmaApp.prise.confirmee";
    public static final String TOPIC_PRISE_MANQUEE      = "pharmaApp.prise.manquee";
    public static final String TOPIC_ALERTE_OBSERVANCE  = "pharmaApp.alerte.observance";
    public static final String TOPIC_TRAITEMENT_CREE    = "pharmaApp.traitement.cree";

    /**
     * Kafka crée les topics automatiquement si absents.
     * partitions(3) = 3 fils de lecture parallèles
     * replicas(1)   = 1 seul broker en dev local (Docker Compose)
     *                 → passer à 3 en production
     */

    @Bean
    public NewTopic topicPriseConfirmee() {
        return TopicBuilder.name(TOPIC_PRISE_CONFIRMEE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic topicPriseManquee() {
        return TopicBuilder.name(TOPIC_PRISE_MANQUEE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic topicAlerteObservance() {
        return TopicBuilder.name(TOPIC_ALERTE_OBSERVANCE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic topicTraitementCree() {
        return TopicBuilder.name(TOPIC_TRAITEMENT_CREE)
                .partitions(3)
                .replicas(1)
                .build();
    }
}