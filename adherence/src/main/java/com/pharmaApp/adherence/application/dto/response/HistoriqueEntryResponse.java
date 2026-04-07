
// ─────────────────────────────────────────────────────────────
// FICHIER 2 : HistoriqueEntryResponse.java
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.adherence.application.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueEntryResponse {
    private Long   id;
    private String priseMedicamentId;  // ✅ String UUID
    private String medicamentNom;
    private String dosage;
    private LocalDate datePrise;
    private LocalTime heurePrise;
    private String statut;
    private LocalTime heureConfirmation;
    private Integer delaiMinutes;
    private String notePatient;
}
 