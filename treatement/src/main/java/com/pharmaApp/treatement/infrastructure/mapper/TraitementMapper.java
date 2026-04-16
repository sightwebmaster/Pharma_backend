package com.pharmaApp.treatement.infrastructure.mapper;

import com.pharmaApp.treatement.application.dto.PriseResponse;
import com.pharmaApp.treatement.application.dto.TraitementResponse;
import com.pharmaApp.treatement.domain.model.*;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.*;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TraitementMapper {

    // =================================================================
    // Domain → Response DTO
    // =================================================================

    @Mapping(target = "statut", expression = "java(traitement.getStatut().name())")
    @Mapping(target = "nombrePrisesTotal",
            expression = "java(traitement.getPrises().size())")
    @Mapping(target = "nombrePrisesConfirmees",
            expression = "java((int) traitement.getPrises().stream().filter(p -> \"CONFIRMEE\".equals(p.getStatut().name())).count())")
    @Mapping(target = "nombrePrisesManquees",
            expression = "java((int) traitement.getPrises().stream().filter(p -> \"MANQUEE\".equals(p.getStatut().name())).count())")
    TraitementResponse toResponse(Traitement traitement);

    @Mapping(target = "statut", expression = "java(prise.getStatut().name())")
    PriseResponse toPriseResponse(PrisePlanifiee prise);

    // =================================================================
    // Domain → Entity JPA
    // =================================================================

    @Mapping(target = "statut",
            expression = "java(com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.TraitementEntity.TraitementStatutJpa.valueOf(traitement.getStatut().name()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lignes",    ignore = true)
    @Mapping(target = "prises",    ignore = true)
    TraitementEntity toEntity(Traitement traitement);

    @Mapping(target = "traitement", ignore = true)
    @Mapping(target = "createdAt",  ignore = true)
    @Mapping(target = "frequenceParJour",
            expression = "java(ligne.getHeuresPrise() != null ? ligne.getHeuresPrise().size() : 0)")
    LigneMedicamentEntity ligneToEntity(LigneMedicament ligne);

    // =================================================================
    // Entity JPA → Domain (MANUEL — MapStruct ne peut pas faire tout seul)
    // =================================================================

    default Traitement toDomain(TraitementEntity entity) {
        if (entity == null) return null;

        // Mapper les lignes
        List<LigneMedicament> lignes = new ArrayList<>();
        if (entity.getLignes() != null) {
            for (LigneMedicamentEntity le : entity.getLignes()) {
                lignes.add(ligneEntityToDomain(le));
            }
        }

        // ✅ Mapper les prises (c'était ça qui manquait)
        List<PrisePlanifiee> prises = new ArrayList<>();
        if (entity.getPrises() != null) {
            for (PrisePlanifieeEntity pe : entity.getPrises()) {
                prises.add(priseEntityToDomain(pe));
            }
        }

        return Traitement.reconstituer(
                entity.getId(),
                entity.getPatientUserId(),
                entity.getPharmacienUserId(),
                TraitementStatut.valueOf(entity.getStatut().name()),
                entity.getDateDebut(),
                entity.getDateFin(),
                entity.getMotif(),
                entity.getNotesPharmacien(),
                entity.getVersion(),
                lignes,
                prises
        );
    }

    default LigneMedicament ligneEntityToDomain(LigneMedicamentEntity e) {
        if (e == null) return null;
        return new LigneMedicament(
                e.getId(),
                e.getMedicamentId(),
                e.getMedicamentNom(),
                e.getPrincipeActif(),
                e.getDosage(),
                e.getDureeJours(),
                e.getHeuresPrise(),
                e.getInstructions()
        );
    }

    default PrisePlanifiee priseEntityToDomain(PrisePlanifieeEntity e) {
        if (e == null) return null;
        return PrisePlanifiee.reconstituer(
                e.getId(),
                e.getTraitement().getId(),
                e.getLigneMedicament().getId(),
                e.getPatientUserId(),
                e.getLigneMedicament().getMedicamentNom(),
                e.getHeurePrevue(),
                e.getHeureReelle(),
                PriseStatut.valueOf(e.getStatut().name())
        );
    }
}