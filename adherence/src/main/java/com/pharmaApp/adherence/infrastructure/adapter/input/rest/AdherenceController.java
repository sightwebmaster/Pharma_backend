package com.pharmaApp.adherence.infrastructure.adapter.input.rest;

import com.pharmaApp.adherence.application.dto.response.AdherenceSummaryResponse;
import com.pharmaApp.adherence.application.dto.response.HistoriqueEntryResponse;
import com.pharmaApp.adherence.domain.port.input.AdherenceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping({"/api/adherence", "/api/observance"})
@RequiredArgsConstructor
public class AdherenceController {

    private final AdherenceUseCase adherenceUseCase;

    @GetMapping("/{patientUserId}/summary")
    public ResponseEntity<AdherenceSummaryResponse> getPatientGlobalSummary(
            @PathVariable String patientUserId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        List<AdherenceSummaryResponse> summaries = adherenceUseCase.getAllSummariesByPatient(patientUserId);

        if (summaries.isEmpty()) {
            return ResponseEntity.ok(
                    AdherenceSummaryResponse.builder()
                            .patientUserId(patientUserId)
                            .taux7j(0)
                            .taux30j(0)
                            .taux90j(0)
                            .tauxGlobal(0)
                            .consecutiveMissed(0)
                            .totalPrises(0)
                            .prisesConfirmees(0)
                            .prisesManquees(0)
                            .lastCalculated(LocalDate.now())
                            .niveauObservance("AUCUNE_DONNEE")
                            .alerteEnvoyee(false)
                            .build()
            );
        }

        double avg7 = summaries.stream().mapToDouble(AdherenceSummaryResponse::getTaux7j).average().orElse(0);
        double avg30 = summaries.stream().mapToDouble(AdherenceSummaryResponse::getTaux30j).average().orElse(0);
        double avg90 = summaries.stream().mapToDouble(AdherenceSummaryResponse::getTaux90j).average().orElse(0);
        double avgGlobal = summaries.stream().mapToDouble(AdherenceSummaryResponse::getTauxGlobal).average().orElse(0);
        int totalPrises = summaries.stream().mapToInt(AdherenceSummaryResponse::getTotalPrises).sum();
        int prisesConfirmees = summaries.stream().mapToInt(AdherenceSummaryResponse::getPrisesConfirmees).sum();
        int prisesManquees = summaries.stream().mapToInt(AdherenceSummaryResponse::getPrisesManquees).sum();
        int consecutiveMissed = summaries.stream().mapToInt(AdherenceSummaryResponse::getConsecutiveMissed).max().orElse(0);
        boolean alerteEnvoyee = summaries.stream().anyMatch(AdherenceSummaryResponse::isAlerteEnvoyee);
        LocalDate lastCalculated = summaries.stream()
                .map(AdherenceSummaryResponse::getLastCalculated)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        String niveau;
        if (avgGlobal >= 90) {
            niveau = "EXCELLENT";
        } else if (avgGlobal >= 75) {
            niveau = "BON";
        } else if (avgGlobal >= 50) {
            niveau = "A_SURVEILLER";
        } else {
            niveau = "CRITIQUE";
        }

        return ResponseEntity.ok(
                AdherenceSummaryResponse.builder()
                        .patientUserId(patientUserId)
                        .taux7j(avg7)
                        .taux30j(avg30)
                        .taux90j(avg90)
                        .tauxGlobal(avgGlobal)
                        .consecutiveMissed(consecutiveMissed)
                        .totalPrises(totalPrises)
                        .prisesConfirmees(prisesConfirmees)
                        .prisesManquees(prisesManquees)
                        .lastCalculated(lastCalculated)
                        .niveauObservance(niveau)
                        .alerteEnvoyee(alerteEnvoyee)
                        .build()
        );
    }

    @GetMapping("/{patientUserId}/historique/{traitementId}")
    public ResponseEntity<List<HistoriqueEntryResponse>> getHistoriqueTraitement(
            @PathVariable String patientUserId,
            @PathVariable String traitementId) {
        return ResponseEntity.ok(adherenceUseCase.getHistorique(patientUserId, traitementId));
    }

    @GetMapping("/{patientUserId}/traitement/{traitementId}")
    public ResponseEntity<AdherenceSummaryResponse> getTraitementSummary(
            @PathVariable String patientUserId,
            @PathVariable String traitementId) {
        return ResponseEntity.ok(adherenceUseCase.getSummary(patientUserId, traitementId));
    }
}
