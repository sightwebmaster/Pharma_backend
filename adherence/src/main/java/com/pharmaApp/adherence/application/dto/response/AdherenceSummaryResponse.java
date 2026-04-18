package com.pharmaApp.adherence.application.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdherenceSummaryResponse {

    private String traitementId;    // ✅ String UUID
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
    private String niveauObservance;
    private boolean alerteEnvoyee;
}