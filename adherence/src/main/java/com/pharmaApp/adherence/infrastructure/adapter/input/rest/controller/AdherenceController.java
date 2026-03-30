package com.pharmaApp.adherence.infrastructure.adapter.input.rest.controller;

import com.pharmaApp.adherence.application.dto.request.EnregistrerPriseRequest;
import com.pharmaApp.adherence.application.dto.response.*;
import com.pharmaApp.adherence.domain.port.input.AdherenceUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Adaptateur d'entrée REST — expose les endpoints du service Adherence.
 *
 * Sécurité : JWT Keycloak (role PATIENT / PHARMACIEN)
 *
 * Endpoints :
 *   POST   /adherence/prises                          → UC1 (tests directs)
 *   GET    /adherence/{patientId}/traitement/{tid}    → UC2 summary
 *   GET    /adherence/{patientId}/traitement/{tid}/historique → UC3
 *   GET    /adherence/pharmacien/summaries            → UC4
 *   GET    /adherence/patient/summaries               → UC5
 *   POST   /adherence/{patientId}/traitement/{tid}/recalculate → UC6
 */
@RestController
@RequestMapping("/adherence")
@RequiredArgsConstructor
public class AdherenceController {

    private final AdherenceUseCase adherenceUseCase;

    /**
     * UC1 — Enregistrer une prise manuellement (utile pour tests / intégration directe).
     * En production, cet endpoint est appelé via Kafka automatiquement.
     * Réservé aux PHARMACIENS ou intégrations internes.
     */
    @PostMapping("/prises")
    @PreAuthorize("hasRole('PHARMACIEN') or hasRole('SYSTEM')")
    public ResponseEntity<AdherenceRecordResponse> enregistrerPrise(
            @Valid @RequestBody EnregistrerPriseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adherenceUseCase.enregistrerPrise(request));
    }

    /**
     * UC2 — Résumé d'observance (taux 7j / 30j / 90j / global).
     * Accessible par le patient lui-même, son pharmacien, et son proche.
     */
    @GetMapping("/{patientId}/traitement/{traitementId}")
    @PreAuthorize("hasAnyRole('PATIENT','PHARMACIEN','PROCHE')")
    public ResponseEntity<AdherenceSummaryResponse> getSummary(
            @PathVariable String patientId,
            @PathVariable Long traitementId,
            @AuthenticationPrincipal Jwt jwt) {

        // Un patient ne peut voir que ses propres données
        verifyPatientAccess(jwt, patientId);

        return ResponseEntity.ok(adherenceUseCase.getSummary(patientId, traitementId));
    }

    /**
     * UC3 — Historique complet des prises pour un traitement.
     */
    @GetMapping("/{patientId}/traitement/{traitementId}/historique")
    @PreAuthorize("hasAnyRole('PATIENT','PHARMACIEN','PROCHE')")
    public ResponseEntity<List<HistoriqueEntryResponse>> getHistorique(
            @PathVariable String patientId,
            @PathVariable Long traitementId,
            @AuthenticationPrincipal Jwt jwt) {

        verifyPatientAccess(jwt, patientId);

        return ResponseEntity.ok(adherenceUseCase.getHistorique(patientId, traitementId));
    }

    /**
     * UC4 — Tous les résumés des patients d'un pharmacien.
     * Tableau de bord pharmacien — triés par taux croissant (les plus critiques en premier).
     */
    @GetMapping("/pharmacien/summaries")
    @PreAuthorize("hasRole('PHARMACIEN')")
    public ResponseEntity<List<AdherenceSummaryResponse>> getSummariesByPharmacien(
            @AuthenticationPrincipal Jwt jwt) {
        String pharmacienId = jwt.getSubject();
        return ResponseEntity.ok(adherenceUseCase.getAllSummariesByPharmacien(pharmacienId));
    }

    /**
     * UC5 — Tous les traitements + taux du patient connecté.
     */
    @GetMapping("/patient/summaries")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<AdherenceSummaryResponse>> getSummariesByPatient(
            @AuthenticationPrincipal Jwt jwt) {
        String patientId = jwt.getSubject();
        return ResponseEntity.ok(adherenceUseCase.getAllSummariesByPatient(patientId));
    }

    /**
     * UC6 — Forcer le recalcul des taux (admin / pharmacien).
     */
    @PostMapping("/{patientId}/traitement/{traitementId}/recalculate")
    @PreAuthorize("hasRole('PHARMACIEN')")
    public ResponseEntity<AdherenceSummaryResponse> recalculate(
            @PathVariable String patientId,
            @PathVariable Long traitementId) {
        return ResponseEntity.ok(adherenceUseCase.recalculerTaux(patientId, traitementId));
    }

    // ── Helper sécurité ────────────────────────────────────────────────────────

    /**
     * Un PATIENT ne peut accéder qu'à ses propres données.
     * Un PHARMACIEN et un PROCHE peuvent accéder à tout (filtrage par leur liste).
     */
    private void verifyPatientAccess(Jwt jwt, String requestedPatientId) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) return;

        boolean isPatient = roles.stream().anyMatch(r ->
                r.equalsIgnoreCase("PATIENT") || r.equalsIgnoreCase("ROLE_PATIENT"));

        if (isPatient && !jwt.getSubject().equals(requestedPatientId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé : vous ne pouvez consulter que vos propres données.");
        }
    }
}
