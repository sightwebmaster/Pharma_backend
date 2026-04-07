package com.pharmaApp.adherence.domain.port.output;

import com.pharmaApp.adherence.domain.model.AdherenceRecord;

import java.util.List;
import java.util.Optional;

/**
 * Port de sortie — Repository Adherence.
 * Implémenté par AdherenceRepositoryAdapter dans la couche infrastructure.
 */
public interface AdherenceRepository {

    AdherenceRecord save(AdherenceRecord record);

    // ✅ String UUID
    Optional<AdherenceRecord> findByPatientAndTraitement(
            String patientUserId, String traitementId);

    List<AdherenceRecord> findAllByPatient(String patientUserId);

    List<AdherenceRecord> findAllByPharmacien(String pharmacienUserId);

    // ✅ String UUID
    boolean existsByPatientAndTraitement(
            String patientUserId, String traitementId);
}