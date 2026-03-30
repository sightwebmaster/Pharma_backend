package com.pharmaApp.adherence.domain.event;

import lombok.*;

import java.time.Instant;

/**
 * Événement publié quand 3 prises manquées consécutives sont détectées.
 * Kafka topic : adherence.alerte-consecutive-missed
 * Consommé par : notification-service (alerte proche)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlerteConsecutiveMissedEvent {
    private String eventId;
    private String patientUserId;
    private String pharmacienUserId;
    private Long   traitementId;
    private int    consecutiveMissed;
    private String dernierMedicamentManque;
    private Instant occurredAt;
}
