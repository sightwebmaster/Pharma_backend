package com.pharmaApp.adherence.domain.event;

import lombok.*;

import java.time.Instant;

/**
 * Événement publié quand le taux d'observance passe sous 70%.
 * Kafka topic : adherence.alerte-observance-critique
 * Consommé par : notification-service (alerte pharmacien + proche)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlerteObservanceCritiqueEvent {
    private String eventId;
    private String patientUserId;
    private String pharmacienUserId;
    private String   traitementId;
    private double taux7j;
    private double taux30j;
    private double tauxGlobal;
    private double seuilCritique;
    private int    consecutiveMissed;
    private Instant occurredAt;
}
