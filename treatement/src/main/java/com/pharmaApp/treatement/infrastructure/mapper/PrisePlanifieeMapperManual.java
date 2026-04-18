package com.pharmaApp.treatement.infrastructure.mapper;

import com.pharmaApp.treatement.domain.model.PrisePlanifiee;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.LigneMedicamentEntity;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.PrisePlanifieeEntity;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.TraitementEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PrisePlanifieeMapperManual {

    public PrisePlanifieeEntity priseToEntity(
            PrisePlanifiee prise,
            TraitementEntity traitementEntity,
            LigneMedicamentEntity ligneEntity) {

        if (prise == null) return null;

        PrisePlanifieeEntity entity = new PrisePlanifieeEntity();
        entity.setId(prise.getId());

        log.info("Mapping prise — prise_id={}, traitement_id={}, ligne_id={}",
                prise.getId(), traitementEntity.getId(), ligneEntity.getId());

        // ✅ Entités managées passées directement — pas de proxy, pas de devinette
        entity.setTraitement(traitementEntity);
        entity.setLigneMedicament(ligneEntity);
        entity.setPatientUserId(prise.getPatientUserId());
        entity.setHeurePrevue(prise.getHeurePrevue());
        entity.setHeureReelle(prise.getHeureReelle());

        if (prise.getStatut() != null) {
            entity.setStatut(PrisePlanifieeEntity.PriseStatutJpa.valueOf(prise.getStatut().name()));
        }

        return entity;
    }
}