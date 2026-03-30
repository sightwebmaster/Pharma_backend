package com.pharmaApp.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Événement publié par treatment-service (Scheduler)
 * Consommé par adherence-service
 *
 * Topic : pharmacare.prise.manquee
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriseManqueeEvent {

    private String eventId;
    private String correlationId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime occurredAt;

    // ── Données métier ──────────────────────────────────────────
    private Long    priseId;
    private Long    traitementId;
    private String  patientUserId;
    private String  medicamentNom;
    private String  principeActif;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime heurePrevue;

    // Délai en minutes entre heurePrevue et détection
    private long    retardMinutes;
}