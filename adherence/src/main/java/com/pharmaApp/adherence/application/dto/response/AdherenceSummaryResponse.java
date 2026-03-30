package com.pharmaApp.adherence.application.dto.response;

import lombok.*;

import java.time.LocalDate;

/**
 * Résumé compact — utilisé dans les tableaux de bord patient / pharmacien.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdherenceSummaryResponse {
    private Long   traitementId;
    private String patientUserId;
    private String pharmacienUserId;
    private double taux7j;
    private double taux30j;
    private double taux90j;
    private double tauxGlobal;
    private int    consecutiveMissed;
    private int    totalPrises;
    private int    prisesConfirmees;
    private int    prisesManquees;
    private LocalDate lastCalculated;

    /** Niveau d'alerte : BON / MOYEN / CRITIQUE */
    private String niveauObservance;

    /** true si une alerte a déjà été envoyée pour ce seuil */
    private boolean alerteEnvoyee;
}
