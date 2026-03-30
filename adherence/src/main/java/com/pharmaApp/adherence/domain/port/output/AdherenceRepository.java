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

    Optional<AdherenceRecord> findByPatientAndTraitement(String patientUserId, Long traitementId);

    List<AdherenceRecord> findAllByPatient(String patientUserId);

    List<AdherenceRecord> findAllByPharmacien(String pharmacienUserId);

    boolean existsByPatientAndTraitement(String patientUserId, Long traitementId);
}
