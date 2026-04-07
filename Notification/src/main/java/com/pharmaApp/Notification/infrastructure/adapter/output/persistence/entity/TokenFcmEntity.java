// ─────────────────────────────────────────────────────────────
// FICHIER 1 : TokenFcmEntity.java
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "tokens_fcm",
        indexes = {
                @Index(name = "idx_token_user", columnList = "user_id, actif")
        }
)
public class TokenFcmEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "user_id", length = 36, nullable = false, columnDefinition = "VARCHAR(36)")
    private String userId;

    @Column(name = "device_token", length = 500, nullable = false)
    private String deviceToken;

    @Column(name = "plateforme", length = 10, nullable = false)
    private String plateforme = "ANDROID";

    @Column(name = "actif", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean actif = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
 