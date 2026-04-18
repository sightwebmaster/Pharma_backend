package com.pharmaApp.treatement.infrastructure.mapper;

import com.pharmaApp.treatement.domain.model.*;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.*;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.LigneMedicamentEntity;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.PrisePlanifieeEntity;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.TraitementEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
@Slf4j
@Component
public class TraitementMapperManuel {

    public Traitement toDomain(TraitementEntity entity) {
        if (entity == null) return null;

        List<LigneMedicament> lignesDomain = entity.getLignes().stream()
                .map(this::ligneToDomain)
                .toList();

        List<PrisePlanifiee> prisesDomain = entity.getPrises().stream()
                .map(this::priseToDomain)
                .toList();

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
                lignesDomain,
                prisesDomain
        );
    }

    private LigneMedicament ligneToDomain(LigneMedicamentEntity e) {
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

    private PrisePlanifiee priseToDomain(PrisePlanifieeEntity e) {
        return new PrisePlanifiee(
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
