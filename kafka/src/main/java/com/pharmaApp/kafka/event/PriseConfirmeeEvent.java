package com.pharmaApp.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Événement publié par treatment-service
 * Consommé par adherence-service
 *
 * Topic : pharmacare.prise.confirmee
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriseConfirmeeEvent {

    // Identifiant unique de l'événement — traçabilité
    private String eventId;

    // Corrélation avec le X-Correlation-Id du gateway
    private String correlationId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime occurredAt;

    // ── Données métier ──────────────────────────────────────────
    private Long    priseId;
    private Long    traitementId;
    private String  patientUserId;      // UUID Keycloak du patient
    private String  medicamentNom;      // snapshot — nom au moment de la prise
    private String  principeActif;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime heurePrevue;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime heureConfirmation;
}