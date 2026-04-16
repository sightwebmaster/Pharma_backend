package com.pharmaApp.treatement.domain.model;

import com.pharmaApp.treatement.domain.exception.AccesDeniedDomainException;
import com.pharmaApp.treatement.domain.model.PriseStatut;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class PrisePlanifiee {

    private  String        id;
    private final String        traitementId;
    private final String        ligneMedicamentId;
    private final String        patientUserId;
    private final String        medicamentNom;
    private final LocalDateTime heurePrevue;

    private PriseStatut   statut;
    private LocalDateTime heureReelle;

    // ✅ Constructeur PRIVÉ — utilisé uniquement par creer()
    private PrisePlanifiee(
            String        traitementId,
            String        ligneMedicamentId,
            String        patientUserId,
            String        medicamentNom,
            LocalDateTime heurePrevue
    ) {
        this.id                = UUID.randomUUID().toString();
        this.traitementId      = traitementId;
        this.ligneMedicamentId = ligneMedicamentId;
        this.patientUserId     = patientUserId;
        this.medicamentNom     = medicamentNom;
        this.heurePrevue       = heurePrevue;
        this.statut            = PriseStatut.PLANIFIEE;
        this.heureReelle       = null;
    }

    // ✅ Static factory — seul point d'entrée public pour créer une prise
    public static PrisePlanifiee creer(
            String        traitementId,
            String        ligneMedicamentId,
            String        patientUserId,
            String        medicamentNom,
            LocalDateTime heurePrevue
    ) {
        Objects.requireNonNull(traitementId,      "traitementId obligatoire");
        Objects.requireNonNull(ligneMedicamentId, "ligneMedicamentId obligatoire");
        Objects.requireNonNull(patientUserId,     "patientUserId obligatoire");
        Objects.requireNonNull(heurePrevue,       "heurePrevue obligatoire");

        return new PrisePlanifiee(  // ✅ return obligatoire
                traitementId,
                ligneMedicamentId,
                patientUserId,
                medicamentNom,
                heurePrevue
        );
    }
    // ✅ Nouveau constructeur de reconstitution
    public PrisePlanifiee(
            String id,
            String traitementId,
            String ligneMedicamentId,
            String patientUserId,
            String medicamentNom,
            LocalDateTime heurePrevue,
            LocalDateTime heureReelle,
            PriseStatut statut) {
        this.id = id;
        this.traitementId = traitementId;
        this.ligneMedicamentId = ligneMedicamentId;
        this.patientUserId = patientUserId;
        this.medicamentNom = medicamentNom;
        this.heurePrevue = heurePrevue;
        this.heureReelle = heureReelle;
        this.statut = statut;
    }


    public static PrisePlanifiee reconstituer(
            String id,
            String traitementId,
            String ligneMedicamentId,
            String patientUserId,
            String medicamentNom,
            LocalDateTime heurePrevue,
            LocalDateTime heureReelle,
            PriseStatut statut) {

        PrisePlanifiee p = new PrisePlanifiee(
                traitementId, ligneMedicamentId, patientUserId,
                medicamentNom, heurePrevue);
        p.id = id;
        p.statut = statut;
        p.heureReelle = heureReelle;
        return p;
    }

    // ... actions métier confirmer(), marquerManquee() inchangées ...
    // ... getters inchangés ...

    // =================================================================
    // ACTIONS MÉTIER
    // =================================================================

    /** R2 — seul le bon patient confirme */
    public void confirmer(String patientIdDemandeur) {
        if (!this.patientUserId.equals(patientIdDemandeur)) {
            throw new AccesDeniedDomainException(
                    "Le patient [" + patientIdDemandeur + "] ne peut pas confirmer " +
                            "la prise du patient [" + this.patientUserId + "]");
        }
        if (this.statut != PriseStatut.PLANIFIEE) {
            throw new IllegalStateException(
                    "Impossible de confirmer une prise déjà [" + this.statut + "]");
        }
        this.statut      = PriseStatut.CONFIRMEE;
        this.heureReelle = LocalDateTime.now();
    }

    public void marquerManquee() {
        if (this.statut != PriseStatut.PLANIFIEE) {
            throw new IllegalStateException(
                    "Impossible de marquer MANQUEE une prise déjà [" + this.statut + "]");
        }
        this.statut = PriseStatut.MANQUEE;
    }

    // =================================================================
    // GETTERS COMPLETS — tous les champs requis par Traitement.java
    // et TraitementJpaAdapter.java
    // =================================================================
    public String        getId()                { return id; }
    public String        getTraitementId()      { return traitementId; }
    public String        getLigneMedicamentId() { return ligneMedicamentId; }
    public String        getPatientUserId()     { return patientUserId; }
    public String        getMedicamentNom()     { return medicamentNom; }
    public LocalDateTime getHeurePrevue()       { return heurePrevue; }
    public PriseStatut   getStatut()            { return statut; }
    public LocalDateTime getHeureReelle()       { return heureReelle; }

    public boolean estPlanifiee() { return statut == PriseStatut.PLANIFIEE; }
    public boolean estConfirmee() { return statut == PriseStatut.CONFIRMEE; }
    public boolean estManquee()   { return statut == PriseStatut.MANQUEE; }
}

