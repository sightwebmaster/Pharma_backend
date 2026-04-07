package com.pharmaApp.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Événement publié par treatment-service
 * Consommé par notification-service
 *
 * Topic : pharmacare.traitement.cree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraitementCreeEvent {

    private String eventId;
    private String correlationId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime occurredAt;

    // ── Données métier ──────────────────────────────────────────
    private Long    traitementId;
    private String  patientUserId;
    private String  pharmacienUserId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateDebut;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFin;

    // Liste des médicaments du traitement — pour planifier les rappels FCM
    private List<PrisePlanifieeInfo> prisesPlanifiees;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrisePlanifieeInfo {
        private Long   priseId;
        private String medicamentNom;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime heurePrevue;
    }
}