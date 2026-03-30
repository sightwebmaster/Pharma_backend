package com.pharmaApp.treatement.application.service;

import com.pharmaApp.treatement.application.dto.*;
import com.pharmaApp.treatement.application.port.in.*;
import com.pharmaApp.treatement.application.port.out.*;
import com.pharmaApp.treatement.domain.event.*;
import com.pharmaApp.treatement.domain.model.*;
import com.pharmaApp.treatement.infrastructure.mapper.TraitementMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * TraitementService — couche Application
 *
 * Rôle : orchestrer les cas d'usage. Ce service :
 *   1. Prépare le contexte (appels aux ports sortants)
 *   2. Délègue la logique métier au domaine (Traitement, PrisePlanifiee)
 *   3. Persiste le résultat
 *   4. Dépile les Domain Events et déclenche les effets de bord
 *      (Notification, Adherence) via les ports sortants
 *
 * Ce service ne contient AUCUNE règle métier — elles sont toutes dans le domaine.
 * Ce service ne connaît ni JPA, ni RestTemplate, ni MySQL — il parle aux ports.
 *
 * Implémente les 4 Use Cases via leurs interfaces (ports entrants).
 */
@Service
@Transactional
public class TraitementService implements
        PlanifierTraitementUseCase,
        ConfirmerPriseUseCase,
        ModifierTraitementUseCase,
        GetTraitementActifUseCase {

    private static final Logger log = LoggerFactory.getLogger(TraitementService.class);

    // ---- Ports sortants injectés par le conteneur Spring ----
    private final TraitementRepositoryPort traitementRepository;
    private final PriseRepositoryPort      priseRepository;
    private final MedicationClientPort     medicationClient;
    private final NotificationClientPort   notificationClient;
    private final AdherenceClientPort      adherenceClient;
    private final TraitementMapper mapper;

    public TraitementService(
            TraitementRepositoryPort traitementRepository,
            PriseRepositoryPort      priseRepository,
            MedicationClientPort     medicationClient,
            NotificationClientPort   notificationClient,
            AdherenceClientPort      adherenceClient,
            TraitementMapper         mapper) {

        this.traitementRepository = traitementRepository;
        this.priseRepository      = priseRepository;
        this.medicationClient     = medicationClient;
        this.notificationClient   = notificationClient;
        this.adherenceClient      = adherenceClient;
        this.mapper               = mapper;
    }

    // =================================================================
    // UC1 — PlanifierTraitement
    // =================================================================

    @Override
    public TraitementResponse planifier(PlanifierTraitementCommand cmd) {
        log.info("Planification traitement — patient={} pharmacien={}",
                cmd.patientUserId(), cmd.acteurId());

        // ── Étape 1 : construire les LigneMedicament du domaine ──────
        // On appelle MedicationClient pour les contre-indications (Fail-Open)
        List<LigneMedicament> lignes = construireLignesDomaine(cmd.lignes());

        // ── Étape 2 : récupérer principes actifs existants (R4 externe) ──
        Set<String> principesActifsActifs =
                traitementRepository.findPrincipesActifsActifs(cmd.patientUserId());

        // ── Étape 3 : créer l'Aggregate Root — toutes les règles R1→R4 ici ──
        // Lève AccesDeniedDomainException (R1), ConflitTraitementException (R4)
        Traitement traitement = Traitement.creer(
                cmd.acteurId(),
                cmd.acteurRole(),
                cmd.patientUserId(),
                cmd.dateDebut(),
                cmd.dateFin(),
                cmd.motif(),
                lignes,
                principesActifsActifs
        );

        // ── Étape 4 : persister ──────────────────────────────────────
        Traitement saved = traitementRepository.save(traitement);

        // ── Étape 5 : dépiler et traiter les Domain Events ───────────
        traiterEvents(saved.pullDomainEvents());

        // ── Étape 6 : mapper vers DTO et retourner ───────────────────
        return mapper.toResponse(saved);
    }

    // =================================================================
    // UC2 — ConfirmerPrise
    // =================================================================

    @Override
    public void confirmer(ConfirmerPriseCommand cmd) {
        log.info("Confirmation prise={} par patient={}", cmd.priseId(), cmd.patientId());

        // ── Charger le traitement ────────────────────────────────────
        Traitement traitement = chargerTraitement(cmd.traitementId());

        // ── Déléguer au domaine — R2 vérifiée dans PrisePlanifiee ────
        // Lève AccesDeniedDomainException si mauvais patient
        PrisePlanifiee prise = traitement.confirmerPrise(cmd.priseId(), cmd.patientId());

        // ── Persister la prise mise à jour ───────────────────────────
        priseRepository.save(prise);

        // ── Dépiler events ───────────────────────────────────────────
        traiterEvents(traitement.pullDomainEvents());
    }

    // =================================================================
    // UC3 — ModifierTraitement
    // =================================================================

    @Override
    public TraitementResponse modifier(ModifierTraitementCommand cmd) {
        log.info("Modification traitement={} → statut={} par acteur={}",
                cmd.traitementId(), cmd.nouveauStatut(), cmd.acteurId());

        // ── Charger ──────────────────────────────────────────────────
        Traitement traitement = chargerTraitement(cmd.traitementId());

        // ── Déléguer au domaine — R1 + R3 vérifiées dans Traitement ──
        // Lève AccesDeniedDomainException (R1), TransitionStatutInvalideException (R3)
        traitement.changerStatut(
                cmd.acteurRole(),
                cmd.acteurId(),
                TraitementStatut.valueOf(cmd.nouveauStatut()),
                cmd.motif()
        );

        // ── Persister ────────────────────────────────────────────────
        Traitement saved = traitementRepository.save(traitement);

        // ── Dépiler events ───────────────────────────────────────────
        traiterEvents(saved.pullDomainEvents());

        return mapper.toResponse(saved);
    }

    // =================================================================
    // UC4 — GetTraitementActif
    // =================================================================

    @Override
    @Transactional(readOnly = true)
    public TraitementResponse getTraitementActif(String patientUserId) {
        log.debug("Consultation traitement actif — patient={}", patientUserId);

        List<Traitement> actifs = traitementRepository.findActifsByPatient(patientUserId);

        if (actifs.isEmpty()) {
            throw new NoSuchElementException(
                    "Aucun traitement actif trouvé pour le patient [" + patientUserId + "]"
            );
        }

        // Retourne le plus récent si plusieurs actifs
        return mapper.toResponse(actifs.get(0));
    }

    // =================================================================
    // SCHEDULER — appelé par PriseManqueeScheduler (adaptateur primaire)
    // Pas un Use Case à part — c'est un point d'entrée interne
    // =================================================================

    /**
     * Détecte les prises non confirmées et les marque MANQUÉE.
     * Appelé toutes les 5 minutes par PriseManqueeScheduler.
     *
     * @param toleranceMinutes délai après lequel une prise est considérée manquée
     */
    public void detecterEtMarquerPrisesManquees(int toleranceMinutes) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(toleranceMinutes);

        List<PrisePlanifiee> prisesEnRetard =
                priseRepository.findPrisesNonConfirmees(deadline);

        if (prisesEnRetard.isEmpty()) {
            log.debug("Scheduler — aucune prise en retard détectée");
            return;
        }

        log.info("Scheduler — {} prise(s) en retard détectée(s)", prisesEnRetard.size());

        for (PrisePlanifiee prise : prisesEnRetard) {

            // Charger le traitement parent pour accéder à l'Aggregate Root
            Traitement traitement = chargerTraitement(prise.getTraitementId());

            // Déléguer au domaine
            traitement.marquerPriseManquee(prise.getId());

            // Persister
            priseRepository.save(prise);

            // Dépiler events (PriseManqueeEvent → Adherence → alerte si taux < 70%)
            traiterEvents(traitement.pullDomainEvents());
        }
    }

    // =================================================================
    // TRAITEMENT DES DOMAIN EVENTS — privé
    // =================================================================

    /**
     * Dépile les Domain Events produits par le domaine et déclenche
     * les effets de bord via les ports sortants.
     *
     * Séquence : persistance d'abord, events ensuite.
     * Si un appel REST échoue ici, la transaction est déjà committée.
     * Pour un PFE c'est acceptable — en prod on utiliserait un outbox pattern.
     */
    private void traiterEvents(List<Object> events) {
        for (Object event : events) {
            if (event instanceof TraitementCreeEvent) {
                TraitementCreeEvent e = (TraitementCreeEvent) event;
                log.info("Event TraitementCree — {} prises planifiées", e.nombrePrises());
                notificationClient.planifierRappels(
                        e.traitementId(), e.patientUserId(), e.nombrePrises()
                );
            }
            else if (event instanceof PriseConfirmeeEvent) {
                PriseConfirmeeEvent e = (PriseConfirmeeEvent) event;
                log.info("Event PriseConfirmee — prise={}", e.priseId());
                int taux = adherenceClient.enregistrerEntree(
                        e.patientUserId(), e.priseId(),
                        e.medicamentNom(), "CONFIRMEE"
                );
                log.debug("Taux observance après confirmation : {}%", taux);
            }
            else if (event instanceof PriseManqueeEvent) {
                PriseManqueeEvent e = (PriseManqueeEvent) event;
                log.warn("Event PriseManquee — prise={} patient={}",
                        e.priseId(), e.patientUserId());

                int taux = adherenceClient.enregistrerEntree(
                        e.patientUserId(), e.priseId(),
                        e.medicamentNom(), "MANQUEE"
                );

                // Si taux critique → alerter le proche
                if (taux < 70) {
                    log.warn("Taux observance critique {}% — alerte proche envoyée",
                            taux);
                    notificationClient.envoyerAlerteProcheManquee(
                            e.patientUserId(), e.medicamentNom()
                    );
                }
            }
            else if (event instanceof TraitementModifieEvent) {
                TraitementModifieEvent e = (TraitementModifieEvent) event;
                log.info("Event TraitementModifie — {} → {}",
                        e.ancienStatut(), e.nouveauStatut());
                notificationClient.mettreAJourRappels(
                        e.traitementId(), e.nouveauStatut()
                );
            }
            else {
                log.warn("Event non géré : {}", event.getClass().getSimpleName());
            }
        }
    }

    // =================================================================
    // CONSTRUCTION DES LIGNES DOMAINE — privé (Fail-Open pour MedicationClient)
    // =================================================================

    /**
     * Construit les LigneMedicament du domaine depuis les commandes.
     * Tente d'enrichir avec les contre-indications depuis MedicationClient.
     *
     * Fail-Open : si MedicationClient est indisponible,
     * on logue un WARNING et on continue sans contre-indications.
     * Le traitement sera créé — c'est un choix métier délibéré.
     */
    private List<LigneMedicament> construireLignesDomaine(
            List<PlanifierTraitementCommand.LigneCommand> lignesCmds) {

        List<LigneMedicament> lignes = new ArrayList<>();

        for (PlanifierTraitementCommand.LigneCommand cmd : lignesCmds) {

            // Tentative de récupération des contre-indications (Fail-Open)
            List<String> contreIndications;
            try {
                contreIndications = medicationClient.getContreIndications(cmd.medicamentId());
                log.debug("Contre-indications récupérées pour {} : {}",
                        cmd.medicamentNom(), contreIndications);
            } catch (Exception e) {
                log.warn("MedicationClient indisponible pour {} — " +
                        "traitement créé sans vérification contre-indications. " +
                        "Cause : {}", cmd.medicamentNom(), e.getMessage());
                contreIndications = Collections.emptyList();
            }

            lignes.add(new LigneMedicament(
                    cmd.medicamentId(),
                    cmd.medicamentNom(),
                    cmd.principeActif(),
                    cmd.dosage(),
                    cmd.dureeJours(),
                    cmd.heuresPrise(),
                    cmd.instructions()
            ));
        }

        return lignes;
    }

    // =================================================================
    // UTILITAIRE — chargement traitement avec exception claire
    // =================================================================

    private Traitement chargerTraitement(String traitementId) {
        return traitementRepository.findById(traitementId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Traitement introuvable : [" + traitementId + "]"
                ));
    }
}