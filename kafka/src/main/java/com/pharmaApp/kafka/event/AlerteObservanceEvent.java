package com.pharmaApp.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Événement publié par adherence-service
 * Consommé par notification-service
 *
 * Topic : pharmacare.alerte.observance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlerteObservanceEvent {

    private String eventId;
    private String correlationId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime occurredAt;

    // ── Données métier ──────────────────────────────────────────
    private String  patientUserId;
    private String  patientNom;           // snapshot pour la notif
    private double  tauxObservance;       // ex: 0.65 = 65%
    private int     nombrePrisesManquees;
    private int     periodeJours;         // calcul sur 7j / 30j

    // Proches à alerter — récupérés par adherence-service via user-service
    private List<String> prochesUserIds;

    // Pharmacien responsable du traitement
    private String  pharmacienUserId;

    // Niveau d'alerte
    private NiveauAlerte niveauAlerte;

    public enum NiveauAlerte {
        WARNING,   // 70% > taux > 50%
        CRITICAL   // taux < 50%
    }
}