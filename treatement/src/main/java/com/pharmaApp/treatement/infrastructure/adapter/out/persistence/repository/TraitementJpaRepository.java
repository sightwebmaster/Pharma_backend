package com.pharmaApp.treatement.infrastructure.adapter.out.persistence.repository;


import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.TraitementEntity;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.TraitementEntity.TraitementStatutJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


/**
 * Repository Spring Data JPA — table traitement
 * Implémente le port sortant TraitementRepositoryPort (via TraitementJpaAdapter)
 */
public interface TraitementJpaRepository
        extends JpaRepository<TraitementEntity, String> {

    /** Traitement actif d'un patient — appelé par recommendation-service */
    Optional<TraitementEntity> findFirstByPatientUserIdAndStatut(
            String patientUserId,
            TraitementStatutJpa statut);


    /** Tous les traitements actifs d'un patient */
    List<TraitementEntity> findByPatientUserIdAndStatut(
            String patientUserId,
            TraitementStatutJpa statut);

    /**
     * Requête de détection de chevauchement (Règle R4).
     * Logique : deux périodes se chevauchent si
     *   dateDebut_existant <= dateFin_nouveau
     *   ET dateFin_existant >= dateDebut_nouveau
     */
    @Query("""
        SELECT t FROM TraitementEntity t
        WHERE t.patientUserId = :patientId
          AND t.statut = 'ACTIF'
          AND t.dateDebut <= :nouvelleFin
          AND t.dateFin   >= :nouveauDebut
    """)
    List<TraitementEntity> findChevauchements(
            @Param("patientId")    String patientId,
            @Param("nouveauDebut") LocalDate nouveauDebut,
            @Param("nouvelleFin")  LocalDate nouvelleFin);
}
