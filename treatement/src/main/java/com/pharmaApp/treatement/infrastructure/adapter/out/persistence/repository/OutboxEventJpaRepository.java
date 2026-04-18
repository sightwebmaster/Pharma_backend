package com.pharmaApp.treatement.infrastructure.adapter.out.persistence.repository;

import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventJpaRepository
        extends JpaRepository<OutboxEventEntity, String> {

    /**
     * Récupère les 50 premiers events PENDING ordonnés par date de création.
     * Le batch de 50 évite de charger toute la table si un backlog s'accumule.
     */
    List<OutboxEventEntity> findTop50ByStatusOrderByCreatedAtAsc(
            OutboxEventEntity.OutboxStatus status
    );
}