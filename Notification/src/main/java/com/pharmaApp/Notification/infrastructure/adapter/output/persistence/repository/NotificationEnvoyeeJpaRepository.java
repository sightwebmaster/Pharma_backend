
// ─────────────────────────────────────────────────────────────
// FICHIER 4 : NotificationEnvoyeeJpaRepository.java
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.infrastructure.adapter.output.persistence.repository;

import com.pharmaApp.Notification.infrastructure.adapter.output.persistence.entity.NotificationEnvoyeeEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationEnvoyeeJpaRepository
        extends JpaRepository<NotificationEnvoyeeEntity, String> {

    List<NotificationEnvoyeeEntity> findByUserIdOrderByEnvoyeADesc(
            String userId, Pageable pageable);
}