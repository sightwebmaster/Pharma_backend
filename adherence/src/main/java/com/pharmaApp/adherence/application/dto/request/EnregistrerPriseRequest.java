package com.pharmaApp.adherence.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de la requête d'enregistrement d'une prise.
 * Peut venir du consumer Kafka OU d'un appel REST direct (tests).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerPriseRequest {

    @NotNull
    private Long priseMedicamentId;

    @NotNull
    private Long traitementId;

    @NotBlank
    private String patientUserId;

    @NotBlank
    private String pharmacienUserId;

    @NotBlank
    private String medicamentNom;

    private String dosage;

    @NotNull
    private LocalDate datePrise;

    @NotNull
    private LocalTime heurePrise;

    /** Null si MANQUE */
    private LocalTime heureConfirmation;

    /** "CONFIRME" ou "MANQUE" */
    @NotBlank
    private String statut;

    private String notePatient;
}
