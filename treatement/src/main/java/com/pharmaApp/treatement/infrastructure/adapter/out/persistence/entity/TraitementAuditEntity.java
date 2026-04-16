package com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity;


import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité JPA — table "traitement_audit"
 *
 * Table append-only : on ne fait jamais UPDATE ni DELETE ici.
 * Chaque changement de statut d'un traitement crée une nouvelle ligne.
 *
 * Utilité pour votre soutenance :
 * - Trace complète pour auditer les décisions médicales
 * - Nécessaire pour RLHF : savoir QUI a modifié QUOI et POURQUOI
 */
@Getter
@Entity
@Table(
        name = "treaitement_audit",
        indexes = {
                @Index(name = "idx_audit_traitement_id", columnList = "traitement_id")
        }
)
public class TraitementAuditEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "treaitement_id", length = 36, nullable = false)
    private String traitementId;

    @Column(name = "ancien_statut", length = 50)
    private String ancienStatut;

    @Column(name = "nouveau_statut", length = 50, nullable = false)
    private String nouveauStatut;

    @Column(name = "modifie_par_id", length = 36, nullable = false)
    private String modifieParId;

    @Enumerated(EnumType.STRING)
    @Column(name = "modifie_par_role", length = 20, nullable = false)
    private RoleActeurJpa modifieParRole;

    @Column(name = "motif_changement", columnDefinition = "TEXT")
    private String motifChangement;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
    }

    public enum RoleActeurJpa {PATIENT, PHARMACIEN, SYSTEME}

    // ---- Builder statique pour usage lisible dans le service ----
    public static TraitementAuditEntity of(
            String traitementId,
            String ancienStatut,
            String nouveauStatut,
            String acteurId,
            RoleActeurJpa role,
            String motif) {
        TraitementAuditEntity e = new TraitementAuditEntity();
        e.traitementId = traitementId;
        e.ancienStatut = ancienStatut;
        e.nouveauStatut = nouveauStatut;
        e.modifieParId = acteurId;
        e.modifieParRole = role;
        e.motifChangement = motif;
        return e;
    }
}