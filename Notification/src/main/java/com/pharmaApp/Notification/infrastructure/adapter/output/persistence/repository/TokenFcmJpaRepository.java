
// ─────────────────────────────────────────────────────────────
// FICHIER 2 : TokenFcmJpaRepository.java
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.infrastructure.adapter.output.persistence.repository;

import com.pharmaApp.Notification.infrastructure.adapter.output.persistence.entity.TokenFcmEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenFcmJpaRepository extends JpaRepository<TokenFcmEntity, String> {

    Optional<TokenFcmEntity> findByUserIdAndActifTrue(String userId);

    Optional<TokenFcmEntity> findByUserIdAndDeviceToken(String userId, String deviceToken);
}
 