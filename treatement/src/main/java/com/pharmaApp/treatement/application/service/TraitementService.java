package com.pharmaApp.treatement.application.service;

import com.pharmaApp.treatement.application.dto.*;
import com.pharmaApp.treatement.application.port.in.*;
import com.pharmaApp.treatement.application.port.out.*;
import com.pharmaApp.treatement.domain.event.*;
import com.pharmaApp.treatement.domain.model.*;
import com.pharmaApp.treatement.infrastructure.mapper.TraitementMapper;
import com.pharmaApp.treatement.infrastructure.messaging.kafka.producer.TraitementKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        GetTraitementActifUseCase,
        GetPrisesAujourdhuiUseCase {

    private static final Logger log = LoggerFactory.getLogger(TraitementService.class);

    // ---- Ports sortants injectés par le conteneur Spring ----
    private final TraitementRepositoryPort traitementRepository;
    private final PriseRepositoryPort      priseRepository;
    private final MedicationClientPort     medicationClient;
    private final TraitementMapper mapper;
    private final TraitementKafkaProducer kafkaProducer;



    public TraitementService(
            TraitementRepositoryPort traitementRepository,
            PriseRepositoryPort      priseRepository,
            MedicationClientPort     medicationClient,

            TraitementMapper         mapper, TraitementKafkaProducer kafkaProducer) {

        this.traitementRepository = traitementRepository;
        this.priseRepository      = priseRepository;
        this.medicationClient     = medicationClient;
        this.mapper               = mapper;

        this.kafkaProducer = kafkaProducer;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriseResponse> getPrisesAujourdhui(String patientUserId) {
        List<PrisePlanifiee> prises = priseRepository
                .findPrisesAujourdhui(patientUserId);

        return prises.stream()
                .map(mapper::toPriseResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PriseResponse> getToutesLesPrises(String patientUserId) {
        List<PrisePlanifiee> prises = priseRepository
                .findToutesLesPrises(patientUserId);

        return prises.stream()
                .map(mapper::toPriseResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    // =================================================================
    // UC1 — PlanifierTraitement
    // =================================================================
    @Transactional
    @Override
    public TraitementResponse planifier(PlanifierTraitementCommand cmd) {
        log.info("Planification traitement — patient={} pharmacien={}",
                cmd.patientUserId(), cmd.acteurId());

        List<LigneMedicament> lignes = construireLignesDomaine(cmd.lignes());
        Set<String> principesActifsActifs =
                traitementRepository.findPrincipesActifsActifs(cmd.patientUserId());

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

        // ✅ Pull events AVANT save (sinon perdus par la reconstitution mapper)
        List<Object> events = traitement.pullDomainEvents();

        Traitement saved = traitementRepository.save(traitement);

        // ✅ UN SEUL appel
        traiterEvents(events);

        return mapper.toResponse(saved);
    }
    // =================================================================
    // UC2 — ConfirmerPrise
    // =================================================================

    @Override
    @Transactional
    public PriseResponse confirmer(ConfirmerPriseCommand cmd) {
        log.info("Confirmation prise — priseId={} patient={}", cmd.priseId(), cmd.patientId());

        Traitement traitement = traitementRepository
                .findById(cmd.traitementId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Traitement introuvable : " + cmd.traitementId()));

        PrisePlanifiee prise = traitement.confirmerPrise(cmd.priseId(), cmd.patientId());

        LigneMedicament ligne = traitement.getLignes().stream()
                .filter(l -> l.getId().equals(prise.getLigneMedicamentId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Ligne introuvable pour prise " + prise.getId()));

        List<Object> events = traitement.pullDomainEvents();

        // ✅ Update ciblé, pas save() complet
        traitementRepository.updatePriseStatut(prise);

        traiterEvents(events);

        return new PriseResponse(
                prise.getId(),
                prise.getTraitementId(),
                prise.getPatientUserId(),
                ligne.getId(),
                prise.getMedicamentNom(),
                prise.getHeurePrevue(),
                prise.getHeureReelle(),
                prise.getStatut().name()
        );
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
    @Transactional
    public void detecterEtMarquerPrisesManquees(int toleranceMinutes) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(toleranceMinutes);

        List<PrisePlanifiee> prisesEnRetard =
                priseRepository.findPrisesNonConfirmees(deadline);

        if (prisesEnRetard.isEmpty()) {
            log.debug("Scheduler — aucune prise en retard détectée");
            return;
        }

        log.info("Scheduler — {} prise(s) en retard détectée(s)", prisesEnRetard.size());

        // ── Grouper par traitementId pour ne charger chaque traitement qu'UNE fois ──
        Map<String, List<PrisePlanifiee>> prisesParTraitement = prisesEnRetard.stream()
                .collect(Collectors.groupingBy(PrisePlanifiee::getTraitementId));

        int totalMarquees = 0;

        for (Map.Entry<String, List<PrisePlanifiee>> entry : prisesParTraitement.entrySet()) {
            String traitementId = entry.getKey();
            List<PrisePlanifiee> prisesDeCeTraitement = entry.getValue();

            try {
                // Charger le traitement UNE fois (avec ses prises grâce au toDomain corrigé)
                Traitement traitement = chargerTraitement(traitementId);

                // Marquer chaque prise comme manquée
                for (PrisePlanifiee priseEnRetard : prisesDeCeTraitement) {
                    PrisePlanifiee priseMarquee = traitement.marquerPriseManquee(priseEnRetard.getId());

                    // Update DB ciblé (même méthode que pour confirmer)
                    traitementRepository.updatePriseStatut(priseMarquee);

                    totalMarquees++;
                    log.info("Prise marquée MANQUEE — id={} traitement={}",
                            priseMarquee.getId(), traitementId);
                }

                // Publier tous les events PriseManqueeEvent du traitement via outbox
                List<Object> events = traitement.pullDomainEvents();
                traiterEvents(events);

            } catch (Exception e) {
                log.error("Erreur traitement {} — prises non marquées : {}",
                        traitementId, e.getMessage(), e);
                // On continue avec les autres traitements
            }
        }

        log.info("Scheduler terminé — {} prise(s) marquée(s) MANQUEE sur {} détectée(s)",
                totalMarquees, prisesEnRetard.size());
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
        log.info("traiterEvents appelé avec {} events", events.size());
        for (Object event : events) {
            if (event instanceof TraitementCreeEvent e) {
                log.info("Outbox ← TraitementCree — {} prises", e.nombrePrises());
                kafkaProducer.saveToOutbox(e);
            }
            else if (event instanceof PriseConfirmeeEvent e) {
                log.info("Outbox ← PriseConfirmee — prise={}", e.priseId());
                kafkaProducer.saveToOutbox(e);
            }
            else if (event instanceof PriseManqueeEvent e) {
                log.warn("Outbox ← PriseManquee — prise={}", e.priseId());
                kafkaProducer.saveToOutbox(e);
            }
            else if (event instanceof TraitementModifieEvent e) {
                log.info("Outbox ← TraitementModifie — {} → {}",
                        e.ancienStatut(), e.nouveauStatut());
                kafkaProducer.saveToOutbox(e);
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
            List<PlanifierTraitementCommand.LigneMedicament> lignesCmds) {

        List<LigneMedicament> lignes = new ArrayList<>();

        for (PlanifierTraitementCommand.LigneMedicament cmd : lignesCmds) {
            log.debug("Contre-indications récupérées pour {} : {}",
                    cmd.medicamentNom(), cmd.id());


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
                    UUID.randomUUID().toString(),
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
