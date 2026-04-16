package com.pharmaApp.treatement.infrastructure.adapter.out.persistence;

import com.pharmaApp.treatement.application.port.out.PriseRepositoryPort;
import com.pharmaApp.treatement.application.port.out.TraitementRepositoryPort;
import com.pharmaApp.treatement.domain.model.LigneMedicament;
import com.pharmaApp.treatement.domain.model.PrisePlanifiee;
import com.pharmaApp.treatement.domain.model.Traitement;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.*;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.repository.*;
import com.pharmaApp.treatement.infrastructure.mapper.PrisePlanifieeMapperManual;
import com.pharmaApp.treatement.infrastructure.mapper.TraitementMapper;
import com.pharmaApp.treatement.infrastructure.mapper.TraitementMapperManuel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TraitementJpaAdapter
        implements TraitementRepositoryPort, PriseRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(TraitementJpaAdapter.class);

    private final TraitementJpaRepository traitementRepo;
    private final PrisePlanifieeJpaRepository priseRepo;
    private final TraitementMapper mapper;
    private final PrisePlanifieeMapperManual priseMapper;
    private final TraitementMapperManuel TraitmentMapper;

    // ✅ Constructeur corrigé - plus de dépendance circulaire
    public TraitementJpaAdapter(
            TraitementJpaRepository traitementRepo,
            PrisePlanifieeJpaRepository priseRepo,
            TraitementMapper mapper,
            PrisePlanifieeMapperManual priseMapper, TraitementMapperManuel traitmentMapper) {
        this.traitementRepo = traitementRepo;
        this.priseRepo = priseRepo;
        this.mapper = mapper;
        this.priseMapper = priseMapper;
        this.TraitmentMapper = traitmentMapper;
    }

    @Override
    public List<PrisePlanifiee> findPrisesAujourdhui(String patientUserId) {
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        LocalDateTime finJour = LocalDate.now().atTime(23, 59, 59);

        return priseRepo
                .findByPatientUserIdAndHeurePrevueBetween(
                        patientUserId, debutJour, finJour)
                .stream()
                .map(this::priseEntityToDomain)
                .collect(Collectors.toList());
    }

    // ================================================================
    // TraitementRepositoryPort
    // ================================================================

    @Override
    public Traitement save(Traitement traitement) {

        // ── 1. Mapper le traitement et ses lignes ───────────────────
        TraitementEntity entity = mapper.toEntity(traitement);

        Map<String, LigneMedicamentEntity> ligneParNom = new LinkedHashMap<>();
        List<LigneMedicamentEntity> lignesEntities = new ArrayList<>();

        for (LigneMedicament ligne : traitement.getLignes()) {
            LigneMedicamentEntity ligneEntity = mapper.ligneToEntity(ligne);
            ligneEntity.setTraitement(entity);
            lignesEntities.add(ligneEntity);
            ligneParNom.put(ligne.getMedicamentNom(), ligneEntity);
            log.info("Ligne préparée — id={} nom={}",
                    ligneEntity.getId(), ligneEntity.getMedicamentNom());
        }
        entity.setLignes(lignesEntities);

        // ── 2. PERSISTER + FLUSH IMMÉDIAT ───────────────────────────
        // Cela force INSERT traitement + INSERT lignes en DB.
        // Les lignes deviennent des entités MANAGÉES par Hibernate.
        TraitementEntity saved = traitementRepo.saveAndFlush(entity);

        // ── 3. Reconstruire la map à partir des lignes MANAGÉES ─────
        Map<String, LigneMedicamentEntity> ligneParNomManaged = new LinkedHashMap<>();
        for (LigneMedicamentEntity l : saved.getLignes()) {
            ligneParNomManaged.put(l.getMedicamentNom(), l);
            log.info("Ligne MANAGÉE — id={} nom={}", l.getId(), l.getMedicamentNom());
        }

        // ── 4. Construire les prises avec les lignes managées ───────
        List<PrisePlanifieeEntity> prisesEntities = new ArrayList<>();
        for (PrisePlanifiee prise : traitement.getPrises()) {
            LigneMedicamentEntity ligneEntity = ligneParNomManaged.get(prise.getMedicamentNom());
            if (ligneEntity == null) {
                throw new IllegalStateException(
                        "Aucune ligne managée pour: " + prise.getMedicamentNom());
            }
            log.info("Prise → ligne_id={} (managée)", ligneEntity.getId());

            PrisePlanifieeEntity priseEntity =
                    priseMapper.priseToEntity(prise, saved, ligneEntity);
            prisesEntities.add(priseEntity);

        }

        // ── 5. Persister les prises ─────────────────────────────────
        priseRepo.saveAll(prisesEntities);

        // ── 6. Retourner le domaine reconstitué ─────────────────────
        return TraitmentMapper.toDomain(saved);
    }

    @Override
    public void updatePriseStatut(PrisePlanifiee prise) {
        PrisePlanifieeEntity entity = priseRepo.findById(prise.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Prise introuvable : " + prise.getId()));

        entity.setStatut(PrisePlanifieeEntity.PriseStatutJpa.valueOf(prise.getStatut().name()));
        entity.setHeureReelle(prise.getHeureReelle());  // null si MANQUEE, sinon timestamp si CONFIRMEE

        priseRepo.save(entity);
    }

    @Override
    @Transactional(readOnly = true)  // ← ajoute ça
    public Optional<Traitement> findById(String id) {
        return traitementRepo.findById(id)
                .map(entity -> {
                    // Force le chargement des collections lazy
                    entity.getLignes().size();
                    entity.getPrises().size();
                    return TraitmentMapper.toDomain(entity);
                });
    }

    @Override
    public Set<String> findPrincipesActifsActifs(String patientUserId) {
        return traitementRepo
                .findByPatientUserIdAndStatut(
                        patientUserId,
                        TraitementEntity.TraitementStatutJpa.ACTIF)
                .stream()
                .flatMap(t -> t.getLignes().stream())
                .map(LigneMedicamentEntity::getPrincipeActif)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public List<Traitement> findActifsByPatient(String patientUserId) {
        return traitementRepo
                .findByPatientUserIdAndStatut(
                        patientUserId,
                        TraitementEntity.TraitementStatutJpa.ACTIF)
                .stream()
                .map(TraitmentMapper::toDomain)
                .collect(Collectors.toList());
    }

    // ================================================================
    // PriseRepositoryPort
    // ================================================================

    @Override
    public PrisePlanifiee save(PrisePlanifiee prise) {
        PrisePlanifieeEntity entity = priseRepo.findById(prise.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Prise introuvable [" + prise.getId() + "] — " +
                                "une prise doit être créée via le Traitement parent"));

        entity.setStatut(PrisePlanifieeEntity.PriseStatutJpa.valueOf(
                prise.getStatut().name()));
        if (prise.getHeureReelle() != null) {
            entity.setHeureReelle(prise.getHeureReelle());
        }
        log.info("ligne-med_id5= {},ligne-med_nom5= {}", entity.getLigneMedicament().getMedicamentId(),entity.getLigneMedicament().getMedicamentNom());


        priseRepo.save(entity);
        log.info("prise_entity_ligne-med= {}", entity.getLigneMedicament().getMedicamentNom());
        return prise;
    }

    @Override
    public List<PrisePlanifiee> findPrisesNonConfirmees(LocalDateTime deadline) {
        return priseRepo.findPrisesNonConfirmees(deadline)
                .stream()
                .map(this::priseEntityToDomain)
                .collect(Collectors.toList());
    }

    // ================================================================
    // Utilitaires privés
    // ================================================================

    private PrisePlanifiee priseEntityToDomain(PrisePlanifieeEntity e) {
        return  PrisePlanifiee.creer(
                e.getTraitement().getId(),
                e.getLigneMedicament().getId(),
                e.getPatientUserId(),
                e.getLigneMedicament().getMedicamentNom(), // ✅ position 7 = medicamentNom
                e.getHeurePrevue()
        );
    }
}