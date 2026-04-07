
package com.pharmaApp.treatement.domain.model;

import com.pharmaApp.treatement.domain.event.*;
import com.pharmaApp.treatement.domain.exception.*;
import com.pharmaApp.treatement.domain.model.LigneMedicament;
import com.pharmaApp.treatement.domain.model.TraitementStatut;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Aggregate Root — Traitement (version corrigée)
 *
 * Corrections apportées :
 *  1. Ajout de la méthode statique reconstituer() — utilisée par MapStruct
 *     pour reconstruire l'objet depuis la base sans déclencher les règles métier
 *  2. Tous les getters vérifiés et complets
 *  3. Constructeur package-private ajouté pour reconstitution JPA
 */
@Getter
@Setter
public class Traitement {

    private final String id;
    private final String patientUserId;
    private final String pharmacienUserId;

    private TraitementStatut statut;
    private final LocalDate dateDebut;
    private final LocalDate dateFin;
    private final String motif;
    private String notesPharmacien;

    private final List<LigneMedicament> lignes;
    private final List<PrisePlanifiee> prises;

    private final List<Object> domainEvents = new ArrayList<>();
    private Integer version;

    // =================================================================
    // FACTORY METHOD — création avec règles métier (R1→R4)
    // =================================================================
    public static Traitement creer(
            String pharmacienUserId,
            String acteurRole,
            String patientUserId,
            LocalDate dateDebut,
            LocalDate dateFin,
            String motif,
            List<LigneMedicament> lignesAjouter,
            Set<String> principesActifsActifs) {

        // R1
        if (!"PHARMACIEN".equalsIgnoreCase(acteurRole)) {
            throw new AccesDeniedDomainException(
                    "Seul un pharmacien peut créer un traitement. Rôle reçu : [" + acteurRole + "]");
        }

        Objects.requireNonNull(patientUserId, "patientUserId obligatoire");
        Objects.requireNonNull(dateDebut, "dateDebut obligatoire");
        Objects.requireNonNull(dateFin, "dateFin obligatoire");

        if (dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException(
                    "dateFin [" + dateFin + "] avant dateDebut [" + dateDebut + "]");
        }
        if (lignesAjouter == null || lignesAjouter.isEmpty()) {
            throw new IllegalArgumentException("Un traitement doit contenir au moins un médicament");
        }

        // R4 interne
        verifierConflitInterne(lignesAjouter);
        // R4 externe
        verifierConflitExterne(lignesAjouter, principesActifsActifs);

        Traitement t = new Traitement(
                UUID.randomUUID().toString(),
                patientUserId,
                pharmacienUserId,
                TraitementStatut.ACTIF,
                dateDebut,
                dateFin,
                motif,
                new ArrayList<>(lignesAjouter)
        );

        t.genererToutesLesPrises();

        t.domainEvents.add(new TraitementCreeEvent(
                t.id, patientUserId, pharmacienUserId,
                t.prises.size(), LocalDateTime.now()
        ));

        return t;
    }

    // =================================================================
    // FACTORY METHOD — reconstitution depuis la base (pour MapStruct)
    // Pas de règles métier, pas de génération de prises
    // =================================================================
    public static Traitement reconstituer(
            String id,
            String patientUserId,
            String pharmacienUserId,
            TraitementStatut statut,
            LocalDate dateDebut,
            LocalDate dateFin,
            String motif,
            String notesPharmacien,
            Integer version,
            List<LigneMedicament> lignes,
            List<PrisePlanifiee> prises) {

        Traitement t = new Traitement(
                id, patientUserId, pharmacienUserId,
                statut, dateDebut, dateFin, motif,
                lignes != null ? new ArrayList<>(lignes) : new ArrayList<>()
        );
        t.notesPharmacien = notesPharmacien;
        t.version = version;
        // Ajoute les prises existantes sans les régénérer
        if (prises != null) {
            t.prises.addAll(prises);
        }
        return t;
    }

    // =================================================================
    // CONFIRMER UNE PRISE — R2
    // =================================================================
    public PrisePlanifiee confirmerPrise(String priseId, String patientIdDemandeur) {
        PrisePlanifiee prise = trouverPrise(priseId);
        prise.confirmer(patientIdDemandeur);

        domainEvents.add(new PriseConfirmeeEvent(
                prise.getId(),
                this.id,
                patientIdDemandeur,
                prise.getMedicamentNom(),
                prise.getHeureReelle(),
                LocalDateTime.now()
        ));
        return prise;
    }

    // =================================================================
    // MARQUER PRISE MANQUÉE
    // =================================================================
    public PrisePlanifiee marquerPriseManquee(String priseId) {
        PrisePlanifiee prise = trouverPrise(priseId);
        prise.marquerManquee();

        domainEvents.add(new PriseManqueeEvent(
                prise.getId(),
                this.id,
                prise.getPatientUserId(),
                prise.getMedicamentNom(),
                prise.getHeurePrevue(),
                LocalDateTime.now()
        ));
        return prise;
    }

    // =================================================================
    // CHANGER STATUT — R1 + R3
    // =================================================================
    public void changerStatut(
            String acteurRole,
            String acteurId,
            TraitementStatut nouveauStatut,
            String motif) {

        if (!"PHARMACIEN".equalsIgnoreCase(acteurRole)) {
            throw new AccesDeniedDomainException(
                    "Seul un pharmacien peut modifier le statut d'un traitement.");
        }
        if (!this.statut.peutTransitionnerVers(nouveauStatut)) {
            throw new TransitionStatutInvalideException(this.statut, nouveauStatut);
        }

        String ancienStatut = this.statut.name();
        this.statut = nouveauStatut;
        this.notesPharmacien = motif;

        domainEvents.add(new TraitementModifieEvent(
                this.id, this.patientUserId,
                ancienStatut, nouveauStatut.name(),
                motif, LocalDateTime.now()
        ));
    }

    // =================================================================
    // R4 — détection conflits
    // =================================================================
    private static void verifierConflitInterne(List<LigneMedicament> lignes) {
        Set<String> vus = new HashSet<>();
        for (LigneMedicament ligne : lignes) {
            if (!vus.add(ligne.getPrincipeActif())) {
                throw new ConflitTraitementException(ligne.getPrincipeActif(),
                        "INTERNE — doublon dans la même prescription");
            }
        }
    }

    private static void verifierConflitExterne(
            List<LigneMedicament> lignes,
            Set<String> principesActifsActifs) {

        if (principesActifsActifs == null || principesActifsActifs.isEmpty()) return;
        for (LigneMedicament ligne : lignes) {
            if (principesActifsActifs.contains(ligne.getPrincipeActif())) {
                throw new ConflitTraitementException(ligne.getPrincipeActif(),
                        "EXTERNE — déjà présent dans un traitement actif du patient");
            }
        }
    }

    // =================================================================
    // GÉNÉRATION DES PRISES
    // =================================================================
    private void genererToutesLesPrises() {
        for (LigneMedicament ligne : this.lignes) {
            String ligneId = UUID.randomUUID().toString();
            for (int jour = 0; jour < ligne.getDureeJours(); jour++) {
                LocalDate jourCourant = this.dateDebut.plusDays(jour);
                for (LocalTime heure : ligne.getHeuresPrise()) {
                    this.prises.add(new PrisePlanifiee(
                            this.id,
                            ligneId,
                            this.patientUserId,
                            ligne.getMedicamentNom(),
                            LocalDateTime.of(jourCourant, heure)
                    ));
                }
            }
        }
    }

    // =================================================================
    // UTILITAIRES
    // =================================================================
    private PrisePlanifiee trouverPrise(String priseId) {
        return this.prises.stream()
                .filter(p -> p.getId().equals(priseId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Prise [" + priseId + "] introuvable dans traitement [" + this.id + "]"));
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }

    public Set<String> getPrincipesActifs() {
        Set<String> result = new HashSet<>();
        for (LigneMedicament l : lignes) {
            result.add(l.getPrincipeActif());
        }
        return Collections.unmodifiableSet(result);
    }

    // =================================================================
    // CONSTRUCTEUR PRIVÉ
    // =================================================================
    private Traitement(
            String id,
            String patientUserId,
            String pharmacienUserId,
            TraitementStatut statut,
            LocalDate dateDebut,
            LocalDate dateFin,
            String motif,
            List<LigneMedicament> lignes) {

        this.id = id;
        this.patientUserId = patientUserId;
        this.pharmacienUserId = pharmacienUserId;
        this.statut = statut;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.motif = motif;
        this.lignes = lignes;
        this.prises = new ArrayList<>();
    }
}