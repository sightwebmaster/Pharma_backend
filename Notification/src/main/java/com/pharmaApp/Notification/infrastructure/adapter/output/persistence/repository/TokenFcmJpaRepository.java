
// ─────────────────────────────────────────────────────────────
// FICHIER 2 : TokenFcmJpaRepository.java
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.infrastructure.adapter.output.persistence.repository;

import com.pharmaApp.Notification.infrastructure.adapter.output.persistence.entity.TokenFcmEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenFcmJpaRepository extends JpaRepository<TokenFcmEntity, String> {

    Optional<TokenFcmEntity> findFirstByUserIdAndActifTrueOrderByUpdatedAtDesc(String userId);

    List<TokenFcmEntity> findAllByUserIdAndActifTrue(String userId);

    List<TokenFcmEntity> findAllByDeviceTokenAndActifTrue(String deviceToken);

    Optional<TokenFcmEntity> findByUserIdAndDeviceToken(String userId, String deviceToken);
}
 
