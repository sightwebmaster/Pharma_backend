package com.pharmaApp.treatement.infrastructure.mapper;

import com.pharmaApp.treatement.application.dto.PriseResponse;
import com.pharmaApp.treatement.application.dto.TraitementResponse;
import com.pharmaApp.treatement.domain.model.LigneMedicament;
import com.pharmaApp.treatement.domain.model.PrisePlanifiee;
import com.pharmaApp.treatement.domain.model.Traitement;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.LigneMedicamentEntity;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.PrisePlanifieeEntity;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.TraitementEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-30T23:03:50+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (BellSoft)"
)
@Component
public class TraitementMapperImpl implements TraitementMapper {

    @Override
    public TraitementResponse toResponse(Traitement traitement) {
        if ( traitement == null ) {
            return null;
        }

        String id = null;
        String patientUserId = null;
        String pharmacienUserId = null;
        LocalDate dateDebut = null;
        LocalDate dateFin = null;
        String motif = null;
        List<TraitementResponse.LigneResponse> lignes = null;

        id = traitement.getId();
        patientUserId = traitement.getPatientUserId();
        pharmacienUserId = traitement.getPharmacienUserId();
        dateDebut = traitement.getDateDebut();
        dateFin = traitement.getDateFin();
        motif = traitement.getMotif();
        lignes = ligneMedicamentListToLigneResponseList( traitement.getLignes() );

        String statut = traitement.getStatut().name();
        int nombrePrisesTotal = traitement.getPrises().size();
        int nombrePrisesConfirmees = (int) traitement.getPrises().stream().filter(p -> "CONFIRMEE".equals(p.getStatut().name())).count();
        int nombrePrisesManquees = (int) traitement.getPrises().stream().filter(p -> "MANQUEE".equals(p.getStatut().name())).count();

        TraitementResponse traitementResponse = new TraitementResponse( id, patientUserId, pharmacienUserId, statut, dateDebut, dateFin, motif, nombrePrisesTotal, nombrePrisesConfirmees, nombrePrisesManquees, lignes );

        return traitementResponse;
    }

    @Override
    public TraitementResponse.LigneResponse toLigneResponse(LigneMedicament ligne) {
        if ( ligne == null ) {
            return null;
        }

        String medicamentNom = null;
        String principeActif = null;
        String dosage = null;
        int dureeJours = 0;
        List<LocalTime> heuresPrise = null;

        medicamentNom = ligne.getMedicamentNom();
        principeActif = ligne.getPrincipeActif();
        dosage = ligne.getDosage();
        dureeJours = ligne.getDureeJours();
        List<LocalTime> list = ligne.getHeuresPrise();
        if ( list != null ) {
            heuresPrise = new ArrayList<LocalTime>( list );
        }

        TraitementResponse.LigneResponse ligneResponse = new TraitementResponse.LigneResponse( medicamentNom, principeActif, dosage, dureeJours, heuresPrise );

        return ligneResponse;
    }

    @Override
    public PriseResponse toPriseResponse(PrisePlanifiee prise) {
        if ( prise == null ) {
            return null;
        }

        String id = null;
        String medicamentNom = null;
        LocalDateTime heurePrevue = null;
        LocalDateTime heureReelle = null;

        id = prise.getId();
        medicamentNom = prise.getMedicamentNom();
        heurePrevue = prise.getHeurePrevue();
        heureReelle = prise.getHeureReelle();

        String statut = prise.getStatut().name();

        PriseResponse priseResponse = new PriseResponse( id, medicamentNom, heurePrevue, heureReelle, statut );

        return priseResponse;
    }

    @Override
    public Traitement toDomain(TraitementEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Traitement traitement = creerTraitementDepuisEntity( entity );

        traitement.setVersion( entity.getVersion() );
        traitement.setNotesPharmacien( entity.getNotesPharmacien() );

        traitement.setStatut( com.pharmaApp.treatement.domain.model.TraitementStatut.valueOf(entity.getStatut().name()) );

        return traitement;
    }

    @Override
    public LigneMedicament ligneToDomain(LigneMedicamentEntity entity) {
        if ( entity == null ) {
            return null;
        }

        String medicamentId = null;
        String medicamentNom = null;
        String principeActif = null;
        String dosage = null;
        int dureeJours = 0;
        String instructions = null;

        medicamentId = entity.getMedicamentId();
        medicamentNom = entity.getMedicamentNom();
        principeActif = entity.getPrincipeActif();
        dosage = entity.getDosage();
        if ( entity.getDureeJours() != null ) {
            dureeJours = entity.getDureeJours();
        }
        instructions = entity.getInstructions();

        List<LocalTime> heuresPrise = null;

        LigneMedicament ligneMedicament = new LigneMedicament( medicamentId, medicamentNom, principeActif, dosage, dureeJours, heuresPrise, instructions );

        return ligneMedicament;
    }

    @Override
    public TraitementEntity toEntity(Traitement traitement) {
        if ( traitement == null ) {
            return null;
        }

        TraitementEntity traitementEntity = new TraitementEntity();

        traitementEntity.setId( traitement.getId() );
        traitementEntity.setPatientUserId( traitement.getPatientUserId() );
        traitementEntity.setPharmacienUserId( traitement.getPharmacienUserId() );
        traitementEntity.setDateDebut( traitement.getDateDebut() );
        traitementEntity.setDateFin( traitement.getDateFin() );
        traitementEntity.setMotif( traitement.getMotif() );
        traitementEntity.setNotesPharmacien( traitement.getNotesPharmacien() );
        traitementEntity.setVersion( traitement.getVersion() );
        traitementEntity.setPrises( prisePlanifieeListToPrisePlanifieeEntityList( traitement.getPrises() ) );

        traitementEntity.setStatut( com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.TraitementEntity.TraitementStatutJpa.valueOf(traitement.getStatut().name()) );

        return traitementEntity;
    }

    @Override
    public LigneMedicamentEntity ligneToEntity(LigneMedicament ligne) {
        if ( ligne == null ) {
            return null;
        }

        LigneMedicamentEntity ligneMedicamentEntity = new LigneMedicamentEntity();

        ligneMedicamentEntity.setMedicamentId( ligne.getMedicamentId() );
        ligneMedicamentEntity.setMedicamentNom( ligne.getMedicamentNom() );
        ligneMedicamentEntity.setPrincipeActif( ligne.getPrincipeActif() );
        ligneMedicamentEntity.setDosage( ligne.getDosage() );
        ligneMedicamentEntity.setDureeJours( ligne.getDureeJours() );
        ligneMedicamentEntity.setInstructions( ligne.getInstructions() );

        ligneMedicamentEntity.setFrequenceParJour( ligne.getHeuresPrise() != null ? ligne.getHeuresPrise().size() : 0 );

        return ligneMedicamentEntity;
    }

    @Override
    public PrisePlanifieeEntity priseToEntity(PrisePlanifiee prise) {
        if ( prise == null ) {
            return null;
        }

        PrisePlanifieeEntity prisePlanifieeEntity = new PrisePlanifieeEntity();

        prisePlanifieeEntity.setId( prise.getId() );
        prisePlanifieeEntity.setPatientUserId( prise.getPatientUserId() );
        prisePlanifieeEntity.setHeurePrevue( prise.getHeurePrevue() );
        prisePlanifieeEntity.setHeureReelle( prise.getHeureReelle() );

        prisePlanifieeEntity.setStatut( com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.PrisePlanifieeEntity.PriseStatutJpa.valueOf(prise.getStatut().name()) );

        return prisePlanifieeEntity;
    }

    protected List<TraitementResponse.LigneResponse> ligneMedicamentListToLigneResponseList(List<LigneMedicament> list) {
        if ( list == null ) {
            return null;
        }

        List<TraitementResponse.LigneResponse> list1 = new ArrayList<TraitementResponse.LigneResponse>( list.size() );
        for ( LigneMedicament ligneMedicament : list ) {
            list1.add( toLigneResponse( ligneMedicament ) );
        }

        return list1;
    }

    protected List<PrisePlanifieeEntity> prisePlanifieeListToPrisePlanifieeEntityList(List<PrisePlanifiee> list) {
        if ( list == null ) {
            return null;
        }

        List<PrisePlanifieeEntity> list1 = new ArrayList<PrisePlanifieeEntity>( list.size() );
        for ( PrisePlanifiee prisePlanifiee : list ) {
            list1.add( priseToEntity( prisePlanifiee ) );
        }

        return list1;
    }
}
