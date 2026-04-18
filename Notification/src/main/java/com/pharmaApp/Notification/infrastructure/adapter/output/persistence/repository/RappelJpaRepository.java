
// ─────────────────────────────────────────────────────────────
// FICHIER 2 : RappelJpaRepository.java
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.infrastructure.adapter.output.persistence.repository;

import com.pharmaApp.Notification.infrastructure.adapter.output.persistence.entity.RappelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RappelJpaRepository extends JpaRepository<RappelEntity, String> {

    long countByTraitementId(String traitementId);

    List<RappelEntity> findByStatutAndHeureEnvoiLessThanEqual(
            String statut, LocalDateTime deadline);

    @Modifying
    @Query("UPDATE RappelEntity r SET r.statut = 'ANNULE' " +
            "WHERE r.traitementId = :traitementId AND r.statut = 'PLANIFIE'")
    int annulerParTraitement(@Param("traitementId") String traitementId);
}