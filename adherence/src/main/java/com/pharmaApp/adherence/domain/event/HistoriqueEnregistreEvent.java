package com.pharmaApp.adherence.domain.event;

import lombok.*;

import java.time.Instant;

/**
 * Événement publié quand une prise est enregistrée.
 * Kafka topic : adherence.historique-enregistre
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueEnregistreEvent {
    private String eventId;
    private String patientUserId;
    private Long   traitementId;
    private Long   priseMedicamentId;
    private String medicamentNom;
    private String statut;   // "CONFIRME" | "MANQUE"
    private double tauxGlobal;
    private Instant occurredAt;
}
