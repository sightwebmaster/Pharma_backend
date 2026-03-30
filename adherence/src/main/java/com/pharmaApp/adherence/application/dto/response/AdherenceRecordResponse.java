package com.pharmaApp.adherence.application.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdherenceRecordResponse {
    private Long   id;
    private String patientUserId;
    private Long   traitementId;
    private String pharmacienUserId;
    private double taux7j;
    private double taux30j;
    private double taux90j;
    private double tauxGlobal;
    private int    consecutiveMissed;
    private LocalDate lastCalculated;
    private int    totalEntries;
    private List<HistoriqueEntryResponse> entries;
}
