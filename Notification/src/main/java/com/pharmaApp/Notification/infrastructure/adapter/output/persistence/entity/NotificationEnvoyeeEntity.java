
// ─────────────────────────────────────────────────────────────
// FICHIER 3 : NotificationEnvoyeeEntity.java
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
        name = "notifications_envoyees",
        indexes = {
                @Index(name = "idx_notif_user_date", columnList = "user_id, envoye_a")
        }
)
public class NotificationEnvoyeeEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "user_id", length = 36, nullable = false, columnDefinition = "VARCHAR(36)")
    private String userId;

    @Column(name = "type", length = 30, nullable = false)
    private String type;

    @Column(name = "titre", length = 200, nullable = false)
    private String titre;

    @Column(name = "corps", length = 500, nullable = false)
    private String corps;

    @Column(name = "statut", length = 10, nullable = false)
    private String statut;

    @Column(name = "fcm_message_id", length = 200)
    private String fcmMessageId;

    @Column(name = "envoye_a", nullable = false)
    private LocalDateTime envoyeA = LocalDateTime.now();
}
 