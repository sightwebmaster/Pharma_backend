package com.pharmaApp.adherence.domain.model;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueEntry {

    private Long id;

    private Long adherenceRecordId;

    // ✅ String UUID — aligné avec treatment-service
    private String priseMedicamentId;

    private String medicamentNom;
    private String dosage;
    private LocalDate datePrise;
    private LocalTime heurePrise;
    private StatutPrise statut;
    private LocalTime heureConfirmation;
    private Integer delaiMinutes;
    private String notePatient;
}