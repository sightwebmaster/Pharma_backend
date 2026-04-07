package com.pharmaApp.adherence.infrastructure.config;

import com.pharmaApp.adherence.domain.event.PriseStatusEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.*;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ── Producer ──────────────────────────────────────────────
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,          bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,       StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,     JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS,             false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ── Consumer ──────────────────────────────────────────────
    @Bean
    public ConsumerFactory<String, PriseStatusEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,          bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG,                   "adherence-service-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,          "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,     StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,   JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES,                "*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                PriseStatusEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(config,
                new StringDeserializer(),
                new JsonDeserializer<>(PriseStatusEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PriseStatusEvent>
    kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, PriseStatusEvent>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    // ── Topics publiés par adherence-service ──────────────────
    @Bean public NewTopic topicHistorique() {
        return TopicBuilder.name("adherence.historique-enregistre").partitions(3).replicas(1).build();
    }
    @Bean public NewTopic topicCritique() {
        return TopicBuilder.name("adherence.alerte-observance-critique").partitions(3).replicas(1).build();
    }
    @Bean public NewTopic topicConsecutif() {
        return TopicBuilder.name("adherence.alerte-consecutive-missed").partitions(3).replicas(1).build();
    }

    // ── Topics consommés depuis treatment-service ─────────────
    // ✅ ALIGNÉS avec TraitementKafkaProducer
    @Bean public NewTopic topicPriseConfirmee() {
        return TopicBuilder.name("prise.confirmee").partitions(3).replicas(1).build();
    }
    @Bean public NewTopic topicPriseManquee() {
        return TopicBuilder.name("prise.manquee").partitions(3).replicas(1).build();
    }
}