package com.pharmaApp.adherence.application.service;

import com.pharmaApp.adherence.application.dto.request.EnregistrerPriseRequest;
import com.pharmaApp.adherence.application.dto.response.*;
import com.pharmaApp.adherence.application.mapper.AdherenceMapper;
import com.pharmaApp.adherence.domain.event.*;
import com.pharmaApp.adherence.domain.exception.AdherenceRecordNotFoundException;
import com.pharmaApp.adherence.domain.model.*;
import com.pharmaApp.adherence.domain.port.input.AdherenceUseCase;
import com.pharmaApp.adherence.domain.port.output.AdherenceRepository;
import com.pharmaApp.adherence.domain.port.output.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service applicatif — implémente les Use Cases du domaine Adherence.
 *
 * Orchestration :
 *  1. Reçoit la demande (Kafka ou REST)
 *  2. Récupère ou crée l'AdherenceRecord via le repository
 *  3. Ajoute l'entrée, recalcule les taux (logique domaine)
 *  4. Persiste
 *  5. Publie les événements DDD si nécessaire
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdherenceService implements AdherenceUseCase {

    private final AdherenceRepository  adherenceRepository;
    private final EventPublisher        eventPublisher;
    private final AdherenceMapper       mapper;

    @Value("${adherence.rules.critical-threshold:70}")
    private double criticalThreshold;

    @Value("${adherence.rules.consecutive-missed-limit:3}")
    private int consecutiveMissedLimit;

    // ─────────────────────────────────────────────────────────────────────────
    // UC1 — Enregistrer une prise
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public AdherenceRecordResponse enregistrerPrise(EnregistrerPriseRequest req) {
        log.info("Enregistrement prise — patient={} traitement={} statut={}",
                req.getPatientUserId(), req.getTraitementId(), req.getStatut());

        // 1. Récupérer ou créer l'AdherenceRecord
        AdherenceRecord record = adherenceRepository
                .findByPatientAndTraitement(req.getPatientUserId(), req.getTraitementId())
                .orElseGet(() -> creerNouvelRecord(req));

        // 2. Construire l'entrée historique
        StatutPrise statut = StatutPrise.valueOf(req.getStatut());
        Integer delai = null;
        if (statut == StatutPrise.CONFIRME && req.getHeureConfirmation() != null) {
            delai = (int) ChronoUnit.MINUTES.between(req.getHeurePrise(), req.getHeureConfirmation());
            if (delai < 0) delai = 0; // peut arriver si confirmation dans les minutes suivantes
        }

        HistoriqueEntry entry = HistoriqueEntry.builder()
                .adherenceRecordId(record.getId())
                .priseMedicamentId(req.getPriseMedicamentId())
                .medicamentNom(req.getMedicamentNom())
                .dosage(req.getDosage())
                .datePrise(req.getDatePrise())
                .heurePrise(req.getHeurePrise())
                .statut(statut)
                .heureConfirmation(req.getHeureConfirmation())
                .delaiMinutes(delai)
                .notePatient(req.getNotePatient())
                .build();

        if (record.getEntries() == null) record.setEntries(new ArrayList<>());
        record.getEntries().add(entry);

        // 3. Recalculer les taux (logique domaine pure)
        record.recalculateTaux();

        // 4. Persister
        AdherenceRecord saved = adherenceRepository.save(record);

        // 5. Publier événements DDD
        publierEvenements(saved, entry);

        return mapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC2 — Summary
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AdherenceSummaryResponse getSummary(String patientUserId, Long traitementId) {
        AdherenceRecord record = adherenceRepository
                .findByPatientAndTraitement(patientUserId, traitementId)
                .orElseThrow(() -> new AdherenceRecordNotFoundException(patientUserId, traitementId));

        AdherenceSummaryResponse summary = mapper.toSummary(record);
        summary.setNiveauObservance(computeNiveau(record.getTauxGlobal()));
        return summary;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC3 — Historique
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<HistoriqueEntryResponse> getHistorique(String patientUserId, Long traitementId) {
        AdherenceRecord record = adherenceRepository
                .findByPatientAndTraitement(patientUserId, traitementId)
                .orElseThrow(() -> new AdherenceRecordNotFoundException(patientUserId, traitementId));

        List<HistoriqueEntry> entries = record.getEntries() == null
                ? Collections.emptyList() : record.getEntries();

        // Tri : du plus récent au plus ancien
        return mapper.toEntryResponseList(
                entries.stream()
                        .sorted(Comparator.comparing(HistoriqueEntry::getDatePrise).reversed()
                                .thenComparing(Comparator.comparing(HistoriqueEntry::getHeurePrise).reversed()))
                        .toList()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC4 — Tous les résumés d'un pharmacien
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AdherenceSummaryResponse> getAllSummariesByPharmacien(String pharmacienUserId) {
        return adherenceRepository.findAllByPharmacien(pharmacienUserId)
                .stream()
                .map(r -> {
                    AdherenceSummaryResponse s = mapper.toSummary(r);
                    s.setNiveauObservance(computeNiveau(r.getTauxGlobal()));
                    return s;
                })
                .sorted(Comparator.comparingDouble(AdherenceSummaryResponse::getTauxGlobal))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC5 — Tous les résumés d'un patient
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AdherenceSummaryResponse> getAllSummariesByPatient(String patientUserId) {
        return adherenceRepository.findAllByPatient(patientUserId)
                .stream()
                .map(r -> {
                    AdherenceSummaryResponse s = mapper.toSummary(r);
                    s.setNiveauObservance(computeNiveau(r.getTauxGlobal()));
                    return s;
                })
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC6 — Forcer recalcul
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public AdherenceSummaryResponse recalculerTaux(String patientUserId, Long traitementId) {
        AdherenceRecord record = adherenceRepository
                .findByPatientAndTraitement(patientUserId, traitementId)
                .orElseThrow(() -> new AdherenceRecordNotFoundException(patientUserId, traitementId));

        record.recalculateTaux();
        AdherenceRecord saved = adherenceRepository.save(record);
        AdherenceSummaryResponse s = mapper.toSummary(saved);
        s.setNiveauObservance(computeNiveau(saved.getTauxGlobal()));
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────────────

    private AdherenceRecord creerNouvelRecord(EnregistrerPriseRequest req) {
        log.info("Création AdherenceRecord — patient={} traitement={}",
                req.getPatientUserId(), req.getTraitementId());
        return AdherenceRecord.builder()
                .patientUserId(req.getPatientUserId())
                .traitementId(req.getTraitementId())
                .pharmacienUserId(req.getPharmacienUserId())
                .entries(new ArrayList<>())
                .taux7j(100.0)
                .taux30j(100.0)
                .taux90j(100.0)
                .tauxGlobal(100.0)
                .consecutiveMissed(0)
                .build();
    }

    private void publierEvenements(AdherenceRecord record, HistoriqueEntry lastEntry) {
        String eventId = UUID.randomUUID().toString();

        // Événement 1 : HistoriqueEnregistré (toujours)
        eventPublisher.publish(HistoriqueEnregistreEvent.builder()
                .eventId(eventId)
                .patientUserId(record.getPatientUserId())
                .traitementId(record.getTraitementId())
                .priseMedicamentId(lastEntry.getPriseMedicamentId())
                .medicamentNom(lastEntry.getMedicamentNom())
                .statut(lastEntry.getStatut().name())
                .tauxGlobal(record.getTauxGlobal())
                .occurredAt(Instant.now())
                .build());

        // Événement 2 : AlerteObservanceCritique (si taux < seuil)
        if (record.isTauxCritique(criticalThreshold)) {
            log.warn("🚨 ALERTE OBSERVANCE — patient={} taux7j={} taux30j={} tauxGlobal={}",
                    record.getPatientUserId(), record.getTaux7j(),
                    record.getTaux30j(), record.getTauxGlobal());

            eventPublisher.publish(AlerteObservanceCritiqueEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .patientUserId(record.getPatientUserId())
                    .pharmacienUserId(record.getPharmacienUserId())
                    .traitementId(record.getTraitementId())
                    .taux7j(record.getTaux7j())
                    .taux30j(record.getTaux30j())
                    .tauxGlobal(record.getTauxGlobal())
                    .seuilCritique(criticalThreshold)
                    .consecutiveMissed(record.getConsecutiveMissed())
                    .occurredAt(Instant.now())
                    .build());
        }

        // Événement 3 : AlerteConsecutiveMissed (si N prises consécutives manquées)
        if (record.getConsecutiveMissed() >= consecutiveMissedLimit
                && lastEntry.getStatut() == StatutPrise.MANQUE) {
            log.warn("⚠️  {} PRISES CONSÉCUTIVES MANQUÉES — patient={}",
                    record.getConsecutiveMissed(), record.getPatientUserId());

            eventPublisher.publish(AlerteConsecutiveMissedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .patientUserId(record.getPatientUserId())
                    .pharmacienUserId(record.getPharmacienUserId())
                    .traitementId(record.getTraitementId())
                    .consecutiveMissed(record.getConsecutiveMissed())
                    .dernierMedicamentManque(lastEntry.getMedicamentNom())
                    .occurredAt(Instant.now())
                    .build());
        }
    }

    private String computeNiveau(double taux) {
        if (taux >= 80) return "BON";
        if (taux >= 70) return "MOYEN";
        return "CRITIQUE";
    }
}
