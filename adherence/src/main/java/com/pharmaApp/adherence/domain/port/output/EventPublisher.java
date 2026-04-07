package com.pharmaApp.adherence.domain.port.output;

import com.pharmaApp.adherence.domain.event.AlerteConsecutiveMissedEvent;
import com.pharmaApp.adherence.domain.event.AlerteObservanceCritiqueEvent;
import com.pharmaApp.adherence.domain.event.HistoriqueEnregistreEvent;

/**
 * Port de sortie — Publication des événements DDD vers Kafka.
 * Implémenté par KafkaEventPublisher dans la couche infrastructure.
 */
public interface EventPublisher {

    void publish(HistoriqueEnregistreEvent event);

    void publish(AlerteObservanceCritiqueEvent event);

    void publish(AlerteConsecutiveMissedEvent event);
}
