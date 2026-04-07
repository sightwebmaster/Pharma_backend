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
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdherenceService implements AdherenceUseCase {

    private final AdherenceRepository adherenceRepository;
    private final EventPublisher       eventPublisher;
    private final AdherenceMapper      mapper;

    @Value("${adherence.rules.critical-threshold:70}")
    private double criticalThreshold;

    @Value("${adherence.rules.consecutive-missed-limit:3}")
    private int consecutiveMissedLimit;

    // ── UC1 ───────────────────────────────────────────────────

    @Override
    public AdherenceRecordResponse enregistrerPrise(EnregistrerPriseRequest req) {
        log.info("Enregistrement prise — patient={} traitement={} statut={}",
                req.getPatientUserId(), req.getTraitementId(), req.getStatut());

        AdherenceRecord record = adherenceRepository
                .findByPatientAndTraitement(req.getPatientUserId(), req.getTraitementId())
                .orElseGet(() -> creerNouvelRecord(req));

        // Normalisation statut → CONFIRME / MANQUE (domaine)
        // Le consumer Kafka envoie CONFIRMEE/MANQUEE, le REST envoie CONFIRME/MANQUE
        String statutNormalise = normaliserStatut(req.getStatut());
        StatutPrise statut = StatutPrise.valueOf(statutNormalise);

        Integer delai = null;
        if (statut == StatutPrise.CONFIRME
                && req.getHeureConfirmation() != null
                && req.getHeurePrise() != null) {
            delai = (int) ChronoUnit.MINUTES.between(
                    req.getHeurePrise(), req.getHeureConfirmation());
            if (delai < 0) delai = 0;
        }

        HistoriqueEntry entry = HistoriqueEntry.builder()
                .adherenceRecordId(record.getId())
                .priseMedicamentId(req.getPriseMedicamentId())   // ✅ String
                .medicamentNom(req.getMedicamentNom())
                .dosage(req.getDosage())
                .datePrise(req.getDatePrise() != null
                        ? req.getDatePrise()
                        : java.time.LocalDate.now())             // fallback Kafka
                .heurePrise(req.getHeurePrise())
                .statut(statut)
                .heureConfirmation(req.getHeureConfirmation())
                .delaiMinutes(delai)
                .notePatient(req.getNotePatient())
                .build();

        if (record.getEntries() == null) record.setEntries(new ArrayList<>());
        record.getEntries().add(entry);

        record.recalculateTaux();

        AdherenceRecord saved = adherenceRepository.save(record);
        publierEvenements(saved, entry);
        return mapper.toResponse(saved);
    }

    // ── UC2 ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AdherenceSummaryResponse getSummary(String patientUserId, String traitementId) {
        AdherenceRecord record = adherenceRepository
                .findByPatientAndTraitement(patientUserId, traitementId)
                .orElseThrow(() -> new AdherenceRecordNotFoundException(patientUserId, traitementId));
        AdherenceSummaryResponse s = mapper.toSummary(record);
        s.setNiveauObservance(computeNiveau(record.getTauxGlobal()));
        return s;
    }

    // ── UC3 ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<HistoriqueEntryResponse> getHistorique(String patientUserId, String traitementId) {
        AdherenceRecord record = adherenceRepository
                .findByPatientAndTraitement(patientUserId, traitementId)
                .orElseThrow(() -> new AdherenceRecordNotFoundException(patientUserId, traitementId));
        List<HistoriqueEntry> entries = record.getEntries() == null
                ? Collections.emptyList() : record.getEntries();
        return mapper.toEntryResponseList(
                entries.stream()
                        .sorted(Comparator.comparing(HistoriqueEntry::getDatePrise).reversed())
                        .toList()
        );
    }

    // ── UC4 ───────────────────────────────────────────────────

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

    // ── UC5 ───────────────────────────────────────────────────

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

    // ── UC6 ───────────────────────────────────────────────────

    @Override
    public AdherenceSummaryResponse recalculerTaux(String patientUserId, String traitementId) {
        AdherenceRecord record = adherenceRepository
                .findByPatientAndTraitement(patientUserId, traitementId)
                .orElseThrow(() -> new AdherenceRecordNotFoundException(patientUserId, traitementId));
        record.recalculateTaux();
        AdherenceRecord saved = adherenceRepository.save(record);
        AdherenceSummaryResponse s = mapper.toSummary(saved);
        s.setNiveauObservance(computeNiveau(saved.getTauxGlobal()));
        return s;
    }

    // ── Helpers ───────────────────────────────────────────────

    private AdherenceRecord creerNouvelRecord(EnregistrerPriseRequest req) {
        log.info("Création AdherenceRecord — patient={} traitement={}",
                req.getPatientUserId(), req.getTraitementId());
        return AdherenceRecord.builder()
                .patientUserId(req.getPatientUserId())
                .traitementId(req.getTraitementId())            // ✅ String
                .pharmacienUserId(req.getPharmacienUserId())
                .entries(new ArrayList<>())
                .taux7j(100.0).taux30j(100.0)
                .taux90j(100.0).tauxGlobal(100.0)
                .consecutiveMissed(0)
                .build();
    }

    private void publierEvenements(AdherenceRecord record, HistoriqueEntry lastEntry) {
        String eventId = UUID.randomUUID().toString();

        eventPublisher.publish(HistoriqueEnregistreEvent.builder()
                .eventId(eventId)
                .patientUserId(record.getPatientUserId())
                .traitementId(record.getTraitementId())         // ✅ String
                .priseMedicamentId(lastEntry.getPriseMedicamentId()) // ✅ String
                .medicamentNom(lastEntry.getMedicamentNom())
                .statut(lastEntry.getStatut().name())
                .tauxGlobal(record.getTauxGlobal())
                .occurredAt(Instant.now())
                .build());

        if (record.isTauxCritique(criticalThreshold)) {
            log.warn("ALERTE OBSERVANCE — patient={} tauxGlobal={}",
                    record.getPatientUserId(), record.getTauxGlobal());
            eventPublisher.publish(AlerteObservanceCritiqueEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .patientUserId(record.getPatientUserId())
                    .pharmacienUserId(record.getPharmacienUserId())
                    .traitementId(record.getTraitementId())     // ✅ String
                    .taux7j(record.getTaux7j())
                    .taux30j(record.getTaux30j())
                    .tauxGlobal(record.getTauxGlobal())
                    .seuilCritique(criticalThreshold)
                    .consecutiveMissed(record.getConsecutiveMissed())
                    .occurredAt(Instant.now())
                    .build());
        }

        if (record.getConsecutiveMissed() >= consecutiveMissedLimit
                && lastEntry.getStatut() == StatutPrise.MANQUE) {
            log.warn("{} PRISES CONSÉCUTIVES MANQUÉES — patient={}",
                    record.getConsecutiveMissed(), record.getPatientUserId());
            eventPublisher.publish(AlerteConsecutiveMissedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .patientUserId(record.getPatientUserId())
                    .pharmacienUserId(record.getPharmacienUserId())
                    .traitementId(record.getTraitementId())     // ✅ String
                    .consecutiveMissed(record.getConsecutiveMissed())
                    .dernierMedicamentManque(lastEntry.getMedicamentNom())
                    .occurredAt(Instant.now())
                    .build());
        }
    }

    /**
     * Normalise le statut vers les valeurs du domaine (CONFIRME / MANQUE).
     * Kafka envoie CONFIRMEE/MANQUEE, REST envoie CONFIRME/MANQUE.
     */
    private String normaliserStatut(String statut) {
        if (statut == null) return "MANQUE";
        return switch (statut.toUpperCase()) {
            case "CONFIRMEE", "CONFIRME" -> "CONFIRME";
            case "MANQUEE",  "MANQUE"   -> "MANQUE";
            default -> statut.toUpperCase();
        };
    }

    private String computeNiveau(double taux) {
        if (taux >= 80) return "BON";
        if (taux >= 70) return "MOYEN";
        return "CRITIQUE";
    }
}