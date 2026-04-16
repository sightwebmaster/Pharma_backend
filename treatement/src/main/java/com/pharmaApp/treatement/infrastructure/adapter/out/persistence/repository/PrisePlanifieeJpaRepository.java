package com.pharmaApp.treatement.infrastructure.adapter.out.persistence.repository;

import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.PrisePlanifieeEntity;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.PrisePlanifieeEntity.PriseStatutJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;




public interface PrisePlanifieeJpaRepository
        extends JpaRepository<PrisePlanifieeEntity, String> {

    /**
     * Requête du Scheduler (exécutée toutes les 5 minutes).
     * Retourne toutes les prises PLANIFIÉES dont l'heure prévue
     * est dépassée depuis plus de 30 minutes.
     * L'index idx_prise_statut_heure rend cette requête très rapide.
     */
    @Query("""
        SELECT p FROM PrisePlanifieeEntity p
        WHERE p.statut = 'PLANIFIEE'
          AND p.heurePrevue <= :deadline
    """)
    List<PrisePlanifieeEntity> findPrisesNonConfirmees(
            @Param("deadline") LocalDateTime deadline);

    /** Historique des prises d'un patient (pour l'écran de suivi) */
    List<PrisePlanifieeEntity> findByPatientUserIdOrderByHeurePrevueDesc(
            String patientUserId);

    /** Prises d'un patient filtrées par statut */
    List<PrisePlanifieeEntity> findByPatientUserIdAndStatut(
            String patientUserId,
            PriseStatutJpa statut);
    List<PrisePlanifieeEntity> findByPatientUserIdAndHeurePrevueBetween(
            String patientUserId,
            LocalDateTime debut,
            LocalDateTime fin
    );


}