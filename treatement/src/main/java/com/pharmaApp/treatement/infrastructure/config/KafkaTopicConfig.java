package com.pharmaApp.treatement.infrastructure.config;

import com.pharmaApp.treatement.infrastructure.messaging.kafka.producer.TraitementKafkaProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * KafkaTopicConfig — crée les topics automatiquement au démarrage.
 * Nécessite que KAFKA_AUTO_CREATE_TOPICS_ENABLE=true dans Docker (déjà configuré).
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic topicPriseConfirmee() {
        return TopicBuilder.name(TraitementKafkaProducer.TOPIC_PRISE_CONFIRMEE)
                .partitions(3)   // 3 partitions pour parallélisme
                .replicas(1)     // 1 replica (dev local)
                .build();
    }

    @Bean
    public NewTopic topicPriseManquee() {
        return TopicBuilder.name(TraitementKafkaProducer.TOPIC_PRISE_MANQUEE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic topicTraitementCree() {
        return TopicBuilder.name(TraitementKafkaProducer.TOPIC_TRAITEMENT_CREE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic topicTraitementModifie() {
        return TopicBuilder.name(TraitementKafkaProducer.TOPIC_TRAITEMENT_MODIFIE)
                .partitions(3)
                .replicas(1)
                .build();
    }
}