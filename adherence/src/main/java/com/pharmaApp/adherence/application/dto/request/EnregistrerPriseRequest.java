package com.pharmaApp.adherence.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de la requête d'enregistrement d'une prise.
 *
 * Peut venir :
 *   - Du consumer Kafka (PriseStatusEvent → champs minimaux)
 *   - D'un appel REST direct (tests pharmacien → champs complets)
 *
 * Tous les champs non fournis par Kafka sont optionnels.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerPriseRequest {

    /** ID de la prise dans treatment-service (UUID String) */
    private String priseMedicamentId;   // ✅ String UUID — était Long

    /** ID du traitement dans treatment-service (UUID String) */
    private String traitementId;        // ✅ String UUID — était Long

    @NotBlank
    private String patientUserId;

    /** Optionnel — non fourni par Kafka, fourni par REST */
    private String pharmacienUserId;

    @NotBlank
    private String medicamentNom;

    /** Optionnel */
    private String dosage;

    /** Optionnel — déduit de now() si absent */
    private LocalDate datePrise;

    /** Optionnel */
    private LocalTime heurePrise;

    /** Null si MANQUEE */
    private LocalTime heureConfirmation;

    /** "CONFIRMEE" | "MANQUEE" */
    @NotBlank
    private String statut;

    /** Optionnel */
    private String notePatient;

    private Integer  delaiMinutes;
}