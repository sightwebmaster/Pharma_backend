package com.pharmaApp.adherence.domain.event;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Événement consommé depuis treatment-service.
 * Kafka topic : treatment.prise-confirmee  OU  treatment.prise-manquee
 *
 * Ce DTO correspond à ce que le treatment-service publie.
 * Les deux types d'événements ont la même structure.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriseStatusEvent {
    private Long   priseMedicamentId;
    private Long   traitementId;
    private String patientUserId;
    private String pharmacienUserId;
    private String medicamentNom;
    private String dosage;
    private LocalDate datePrise;
    private LocalTime heurePrise;
    private LocalTime heureConfirmation; // null si MANQUE
    private String statut;               // "CONFIRME" | "MANQUE"
    private String notePatient;
}
