package com.pharmaApp.adherence.domain.port.input;

import com.pharmaApp.adherence.application.dto.request.EnregistrerPriseRequest;
import com.pharmaApp.adherence.application.dto.response.AdherenceRecordResponse;
import com.pharmaApp.adherence.application.dto.response.AdherenceSummaryResponse;
import com.pharmaApp.adherence.application.dto.response.HistoriqueEntryResponse;

import java.util.List;

/**
 * Port d'entrée — Use Cases du domaine Adherence.
 *
 * Implémenté par AdherenceService dans la couche application.
 * Appelé par :
 *   - Le consumer Kafka (événements treatment-service)
 *   - Les contrôleurs REST (lectures patient / pharmacien / proche)
 */
public interface AdherenceUseCase {

    /**
     * UC1 — Enregistrer une prise (confirmée ou manquée).
     * Déclenché par événement Kafka depuis treatment-service.
     * Recalcule les taux et publie les alertes si nécessaire.
     */
    AdherenceRecordResponse enregistrerPrise(EnregistrerPriseRequest request);

    /**
     * UC2 — Obtenir le résumé d'observance d'un patient pour un traitement.
     * Retourne les taux 7j / 30j / 90j + statut courant.
     */
    AdherenceSummaryResponse getSummary(String patientUserId, Long traitementId);

    /**
     * UC3 — Obtenir l'historique complet des prises d'un patient.
     * Accessible par patient, pharmacien ET proche.
     */
    List<HistoriqueEntryResponse> getHistorique(String patientUserId, Long traitementId);

    /**
     * UC4 — Obtenir tous les résumés d'observance d'un pharmacien.
     * Permet au pharmacien de surveiller tous ses patients.
     */
    List<AdherenceSummaryResponse> getAllSummariesByPharmacien(String pharmacienUserId);

    /**
     * UC5 — Obtenir le résumé global d'un patient (tous traitements).
     * Utilisé par le tableau de bord patient.
     */
    List<AdherenceSummaryResponse> getAllSummariesByPatient(String patientUserId);

    /**
     * UC6 — Forcer le recalcul du taux pour un traitement.
     * Utile en cas de correction ou de test.
     */
    AdherenceSummaryResponse recalculerTaux(String patientUserId, Long traitementId);
}
