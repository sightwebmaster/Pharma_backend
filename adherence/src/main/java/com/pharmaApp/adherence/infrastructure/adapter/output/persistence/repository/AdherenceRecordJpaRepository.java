package com.pharmaApp.adherence.infrastructure.adapter.output.persistence.repository;

import com.pharmaApp.adherence.infrastructure.adapter.output.persistence.entity.AdherenceRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdherenceRecordJpaRepository extends JpaRepository<AdherenceRecordEntity, Long> {

    Optional<AdherenceRecordEntity> findByPatientUserIdAndTraitementId(
            String patientUserId, Long traitementId);

    List<AdherenceRecordEntity> findAllByPatientUserId(String patientUserId);

    List<AdherenceRecordEntity> findAllByPharmacienUserId(String pharmacienUserId);

    boolean existsByPatientUserIdAndTraitementId(String patientUserId, Long traitementId);

    /**
     * Requête native pour le tableau de bord pharmacien :
     * retourne les patients avec taux critique (< seuil).
     */
    @Query("SELECT a FROM AdherenceRecordEntity a " +
           "WHERE a.pharmacienUserId = :pharmacienId " +
           "AND a.tauxGlobal < :seuil " +
           "ORDER BY a.tauxGlobal ASC")
    List<AdherenceRecordEntity> findCriticalPatients(
            @Param("pharmacienId") String pharmacienId,
            @Param("seuil") double seuil);
}
