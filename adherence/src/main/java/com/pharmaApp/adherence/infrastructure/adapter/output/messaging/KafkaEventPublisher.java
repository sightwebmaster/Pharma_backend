package com.pharmaApp.adherence.infrastructure.adapter.output.messaging;

import com.pharmaApp.adherence.domain.event.*;
import com.pharmaApp.adherence.domain.port.output.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de sortie — publie les événements DDD sur Kafka.
 *
 * Topics :
 *  adherence.historique-enregistre      → tous services intéressés
 *  adherence.alerte-observance-critique → notification-service
 *  adherence.alerte-consecutive-missed  → notification-service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisher {

    private static final String TOPIC_HISTORIQUE   = "adherence.historique-enregistre";
    private static final String TOPIC_CRITIQUE      = "adherence.alerte-observance-critique";
    private static final String TOPIC_CONSECUTIVE   = "adherence.alerte-consecutive-missed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(HistoriqueEnregistreEvent event) {
        log.debug("📤 Kafka → {} : patient={}", TOPIC_HISTORIQUE, event.getPatientUserId());
        kafkaTemplate.send(TOPIC_HISTORIQUE, event.getPatientUserId(), event);
    }

    @Override
    public void publish(AlerteObservanceCritiqueEvent event) {
        log.warn("📤 Kafka → {} : patient={} taux7j={}%",
                TOPIC_CRITIQUE, event.getPatientUserId(), event.getTaux7j());
        kafkaTemplate.send(TOPIC_CRITIQUE, event.getPatientUserId(), event);
    }

    @Override
    public void publish(AlerteConsecutiveMissedEvent event) {
        log.warn("📤 Kafka → {} : patient={} consecutiveMissed={}",
                TOPIC_CONSECUTIVE, event.getPatientUserId(), event.getConsecutiveMissed());
        kafkaTemplate.send(TOPIC_CONSECUTIVE, event.getPatientUserId(), event);
    }
}
