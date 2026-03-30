package com.pharmaApp.treatement.infrastructure.adapter.out.persistence;

import com.pharmaApp.treatement.application.port.out.PriseRepositoryPort;
import com.pharmaApp.treatement.application.port.out.TraitementRepositoryPort;
import com.pharmaApp.treatement.domain.model.LigneMedicament;
import com.pharmaApp.treatement.domain.model.PrisePlanifiee;
import com.pharmaApp.treatement.domain.model.Traitement;
import com.pharmaApp.treatement.domain.model.TraitementStatut;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.*;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.repository.*;
import com.pharmaApp.treatement.infrastructure.mapper.TraitementMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TraitementJpaAdapter
 *
 * Adaptateur secondaire — implémente les deux ports sortants :
 *   - TraitementRepositoryPort
 *   - PriseRepositoryPort
 *
 * C'est ici que le domaine rencontre JPA.
 * TraitementService ne sait pas que JPA existe — il appelle les ports.
 * Cet adaptateur fait la traduction Domain ↔ Entity via TraitementMapper.
 */
@Component
public class TraitementJpaAdapter
        implements TraitementRepositoryPort, PriseRepositoryPort {

    private final TraitementJpaRepository      traitementRepo;
    private final PrisePlanifieeJpaRepository  priseRepo;
    private final TraitementMapper             mapper;

    public TraitementJpaAdapter(
            TraitementJpaRepository     traitementRepo,
            PrisePlanifieeJpaRepository priseRepo,
            TraitementMapper            mapper) {
        this.traitementRepo = traitementRepo;
        this.priseRepo      = priseRepo;
        this.mapper         = mapper;
    }

    // ================================================================
    // TraitementRepositoryPort
    // ================================================================

    @Override
    public Traitement save(Traitement traitement) {
        // 1. Mapper Domain → Entity principale
        TraitementEntity entity = mapper.toEntity(traitement);

        // 2. Mapper et rattacher les lignes médicament
        List<LigneMedicamentEntity> lignesEntities = new ArrayList<>();
        for (LigneMedicament ligne : traitement.getLignes()) {
            LigneMedicamentEntity ligneEntity = mapper.ligneToEntity(ligne);
            ligneEntity.setTraitement(entity);          // référence circulaire
            lignesEntities.add(ligneEntity);
        }
        entity.setLignes(lignesEntities);

        // 3. Mapper et rattacher les prises planifiées
        List<PrisePlanifieeEntity> prisesEntities = new ArrayList<>();
        for (PrisePlanifiee prise : traitement.getPrises()) {
            PrisePlanifieeEntity priseEntity = mapper.priseToEntity(prise);
            priseEntity.setTraitement(entity);

            // Trouver la ligne correspondante par medicamentId
            lignesEntities.stream()
                    .filter(l -> l.getMedicamentId().equals(
                            trouverMedicamentId(traitement, prise.getLigneMedicamentId())))
                    .findFirst()
                    .ifPresent(priseEntity::setLigneMedicament);

            prisesEntities.add(priseEntity);
        }

        // 4. Persister (cascade ALL gère lignes + prises)
        TraitementEntity saved = traitementRepo.save(entity);

        // 5. Persister les prises séparément (pas de cascade sur prise → ligne)
        priseRepo.saveAll(prisesEntities);

        // 6. Retourner le domaine reconstitué depuis l'entity sauvegardée
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Traitement> findById(String traitementId) {
        return traitementRepo.findById(traitementId)
                .map(mapper::toDomain);
    }

    @Override
    public Set<String> findPrincipesActifsActifs(String patientUserId) {
        // Récupère tous les traitements ACTIFS du patient
        // et extrait les principes actifs de leurs lignes
        return traitementRepo
                .findByPatientUserIdAndStatut(
                        patientUserId,
                        TraitementEntity.TraitementStatutJpa.ACTIF)
                .stream()
                .flatMap(t -> t.getLignes().stream())
                .map(LigneMedicamentEntity::getPrincipeActif)
                .collect(Collectors.toSet());
    }

    @Override
    public List<Traitement> findActifsByPatient(String patientUserId) {
        return traitementRepo
                .findByPatientUserIdAndStatut(
                        patientUserId,
                        TraitementEntity.TraitementStatutJpa.ACTIF)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    // ================================================================
    // PriseRepositoryPort
    // ================================================================

    @Override
    public PrisePlanifiee save(PrisePlanifiee prise) {
        PrisePlanifieeEntity entity = priseRepo.findById(prise.getId())
                .orElse(mapper.priseToEntity(prise));

        // Met à jour le statut et l'heure réelle
        entity.setStatut(PrisePlanifieeEntity.PriseStatutJpa.valueOf(
                prise.getStatut().name()));
        if (prise.getHeureReelle() != null) {
            entity.setHeureReelle(prise.getHeureReelle());
        }

        priseRepo.save(entity);
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

    /**
     * Reconstitue un objet PrisePlanifiee du domaine depuis une entity.
     * Utilisé uniquement pour le scheduler (lecture, pas d'écriture complexe).
     */
    private PrisePlanifiee priseEntityToDomain(PrisePlanifieeEntity e) {
        PrisePlanifiee prise = new PrisePlanifiee(
                e.getTraitement().getId(),
                e.getLigneMedicament().getId(),
                e.getPatientUserId(),
                e.getLigneMedicament().getMedicamentNom(),
                e.getHeurePrevue()
        );
        // Restore l'ID original (pas un nouvel UUID)
        // Note : nécessite un setter d'ID dans PrisePlanifiee pour la reconstitution
        return prise;
    }

    private String trouverMedicamentId(Traitement traitement, String ligneMedicamentId) {
        return traitement.getLignes().stream()
                .filter(l -> l.getMedicamentId().equals(ligneMedicamentId))
                .map(LigneMedicament::getMedicamentId)
                .findFirst()
                .orElse("");
    }
}