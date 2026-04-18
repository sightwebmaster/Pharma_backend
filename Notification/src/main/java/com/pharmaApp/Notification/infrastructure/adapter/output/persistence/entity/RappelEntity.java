
// ─────────────────────────────────────────────────────────────
// FICHIER 1 : RappelEntity.java
// ─────────────────────────────────────────────────────────────
        package com.pharmaApp.Notification.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "rappels", indexes = {
        @Index(name = "idx_rappel_statut_heure", columnList = "statut, heure_envoi"),
        @Index(name = "idx_rappel_patient",      columnList = "patient_user_id"),
        @Index(name = "idx_rappel_traitement",   columnList = "traitement_id")
})
@Getter @Setter
public class RappelEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "traitement_id",   length = 36, nullable = false, columnDefinition = "VARCHAR(36)")
    private String traitementId;

    @Column(name = "patient_user_id", length = 36, nullable = false, columnDefinition = "VARCHAR(36)")
    private String patientUserId;

    @Column(name = "medicament_nom", length = 200, nullable = false)
    private String medicamentNom;

    @Column(name = "heure_envoi", nullable = false)
    private LocalDateTime heureEnvoi;

    @Column(name = "heure_reference", nullable = false)
    private LocalDateTime heureReference;

    @Column(name = "statut", length = 20, nullable = false)
    private String statut = "PLANIFIE";

    @Column(name = "nb_tentatives", nullable = false, columnDefinition = "TINYINT")
    private Integer nbTentatives = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
