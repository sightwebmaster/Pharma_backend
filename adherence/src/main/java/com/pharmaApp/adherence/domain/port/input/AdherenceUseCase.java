package com.pharmaApp.adherence.domain.port.input;

import com.pharmaApp.adherence.application.dto.request.EnregistrerPriseRequest;
import com.pharmaApp.adherence.application.dto.response.AdherenceRecordResponse;
import com.pharmaApp.adherence.application.dto.response.AdherenceSummaryResponse;
import com.pharmaApp.adherence.application.dto.response.HistoriqueEntryResponse;

import java.util.List;

/**
 * Port d'entrée — Use Cases du domaine Adherence.
 *
 * Implémenté par AdherenceService.
 * Appelé par :
 *   - PriseEventConsumer (Kafka) → UC1
 *   - AdherenceController (REST) → UC2 à UC6
 */
public interface AdherenceUseCase {

    /**
     * UC1 — Enregistrer une prise (confirmée ou manquée).
     * Déclenché par événement Kafka depuis treatment-service.
     * Recalcule les taux et publie les alertes si nécessaire.
     */
    AdherenceRecordResponse enregistrerPrise(EnregistrerPriseRequest request);

    /**
     * UC2 — Résumé d'observance d'un patient pour un traitement.
     * traitementId = UUID String (aligné avec treatment-service)
     */
    AdherenceSummaryResponse getSummary(String patientUserId, String traitementId);

    /**
     * UC3 — Historique complet des prises d'un patient.
     * Accessible par patient, pharmacien ET proche.
     */
    List<HistoriqueEntryResponse> getHistorique(String patientUserId, String traitementId);

    /**
     * UC4 — Tous les résumés d'observance gérés par un pharmacien.
     */
    List<AdherenceSummaryResponse> getAllSummariesByPharmacien(String pharmacienUserId);

    /**
     * UC5 — Résumé global d'un patient (tous traitements).
     */
    List<AdherenceSummaryResponse> getAllSummariesByPatient(String patientUserId);

    /**
     * UC6 — Forcer le recalcul du taux pour un traitement.
     */
    AdherenceSummaryResponse recalculerTaux(String patientUserId, String traitementId);
}