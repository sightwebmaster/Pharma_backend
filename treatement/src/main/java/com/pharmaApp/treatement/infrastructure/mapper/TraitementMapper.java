package com.pharmaApp.treatement.infrastructure.mapper;

import com.pharmaApp.treatement.application.dto.TraitementResponse;
import com.pharmaApp.treatement.application.dto.PriseResponse;
import com.pharmaApp.treatement.domain.model.*;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.*;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;

/**
 * TraitementMapper — MapStruct corrigé
 *
 * Correction principale :
 *   Traitement a un constructeur PRIVÉ → MapStruct ne peut pas faire
 *   new Traitement() automatiquement.
 *
 *   Solution : @ObjectFactory sur une méthode qui appelle
 *   Traitement.reconstituer() — MapStruct l'utilise à la place
 *   du constructeur lors du mapping Entity → Domain.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TraitementMapper {

    // =================================================================
    // Domain → Response DTO
    // =================================================================

    @Mapping(target = "statut",
            expression = "java(traitement.getStatut().name())")
    @Mapping(target = "nombrePrisesTotal",
            expression = "java(traitement.getPrises().size())")
    @Mapping(target = "nombrePrisesConfirmees",
            expression = "java((int) traitement.getPrises().stream().filter(p -> \"CONFIRMEE\".equals(p.getStatut().name())).count())")
    @Mapping(target = "nombrePrisesManquees",
            expression = "java((int) traitement.getPrises().stream().filter(p -> \"MANQUEE\".equals(p.getStatut().name())).count())")
    TraitementResponse toResponse(Traitement traitement);

    TraitementResponse.LigneResponse toLigneResponse(LigneMedicament ligne);

    @Mapping(target = "statut", expression = "java(prise.getStatut().name())")
    PriseResponse toPriseResponse(PrisePlanifiee prise);

    // =================================================================
    // Entity JPA → Domain
    //
    // @ObjectFactory : MapStruct appelle cette méthode au lieu de
    // chercher un constructeur public. On délègue à Traitement.reconstituer()
    // =================================================================

    @ObjectFactory
    default Traitement creerTraitementDepuisEntity(TraitementEntity entity) {
        TraitementStatut statut = TraitementStatut.valueOf(entity.getStatut().name());

        // Convertir les lignes entités → lignes domaine
        List<LigneMedicament> lignes = new ArrayList<>();
        if (entity.getLignes() != null) {
            for (LigneMedicamentEntity le : entity.getLignes()) {
                lignes.add(ligneToDomain(le));
            }
        }

        return Traitement.reconstituer(
                entity.getId(),
                entity.getPatientUserId(),
                entity.getPharmacienUserId(),
                statut,
                entity.getDateDebut(),
                entity.getDateFin(),
                entity.getMotif(),
                entity.getNotesPharmacien(),
                entity.getVersion(),
                lignes,
                new ArrayList<>()   // prises chargées séparément si nécessaire
        );
    }

    /**
     * Mapping principal Entity → Domain.
     * MapStruct utilisera @ObjectFactory ci-dessus au lieu du constructeur.
     */
    @Mapping(target = "statut",
            expression = "java(com.pharmaApp.treatement.domain.model.TraitementStatut.valueOf(entity.getStatut().name()))")
    @Mapping(target = "lignes",  ignore = true)   // gérées dans @ObjectFactory
    @Mapping(target = "prises",  ignore = true)
    @Mapping(target = "version", source = "version")
    Traitement toDomain(TraitementEntity entity);

    // LigneMedicamentEntity → LigneMedicament (domaine)
    @Mapping(target = "heuresPrise", ignore = true)  // type LocalTime — géré manuellement
    LigneMedicament ligneToDomain(LigneMedicamentEntity entity);

    // =================================================================
    // Domain → Entity JPA
    // =================================================================

    @Mapping(target = "statut",
            expression = "java(com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.TraitementEntity.TraitementStatutJpa.valueOf(traitement.getStatut().name()))")
    @Mapping(target = "lignes",    ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TraitementEntity toEntity(Traitement traitement);

    @Mapping(target = "traitement",      ignore = true)
    @Mapping(target = "prises",          ignore = true)
    @Mapping(target = "createdAt",       ignore = true)
    @Mapping(target = "frequenceParJour",
            expression = "java(ligne.getHeuresPrise() != null ? ligne.getHeuresPrise().size() : 0)")
    LigneMedicamentEntity ligneToEntity(LigneMedicament ligne);

    @Mapping(target = "traitement",         ignore = true)
    @Mapping(target = "ligneMedicament",    ignore = true)
    @Mapping(target = "statut",
            expression = "java(com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.PrisePlanifieeEntity.PriseStatutJpa.valueOf(prise.getStatut().name()))")
    @Mapping(target = "sourceConfirmation", ignore = true)
    @Mapping(target = "createdAt",          ignore = true)
    @Mapping(target = "updatedAt",          ignore = true)
    PrisePlanifieeEntity priseToEntity(PrisePlanifiee prise);
}