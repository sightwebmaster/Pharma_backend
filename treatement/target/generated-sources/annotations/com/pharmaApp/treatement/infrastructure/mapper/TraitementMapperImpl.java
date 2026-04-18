package com.pharmaApp.treatement.infrastructure.mapper;

import com.pharmaApp.treatement.application.dto.PriseResponse;
import com.pharmaApp.treatement.application.dto.TraitementResponse;
import com.pharmaApp.treatement.domain.model.LigneMedicament;
import com.pharmaApp.treatement.domain.model.PrisePlanifiee;
import com.pharmaApp.treatement.domain.model.Traitement;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.LigneMedicamentEntity;
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
    date = "2026-04-18T03:23:50+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.17 (Eclipse Adoptium)"
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
    public PriseResponse toPriseResponse(PrisePlanifiee prise) {
        if ( prise == null ) {
            return null;
        }

        String idLigneMedicament = null;
        String id = null;
        String traitementId = null;
        String patientUserId = null;
        String medicamentNom = null;
        LocalDateTime heurePrevue = null;
        LocalDateTime heureReelle = null;

        idLigneMedicament = prise.getLigneMedicamentId();
        id = prise.getId();
        traitementId = prise.getTraitementId();
        patientUserId = prise.getPatientUserId();
        medicamentNom = prise.getMedicamentNom();
        heurePrevue = prise.getHeurePrevue();
        heureReelle = prise.getHeureReelle();

        String statut = prise.getStatut().name();

        PriseResponse priseResponse = new PriseResponse( id, traitementId, patientUserId, idLigneMedicament, medicamentNom, heurePrevue, heureReelle, statut );

        return priseResponse;
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

        traitementEntity.setStatut( com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.TraitementEntity.TraitementStatutJpa.valueOf(traitement.getStatut().name()) );

        return traitementEntity;
    }

    @Override
    public LigneMedicamentEntity ligneToEntity(LigneMedicament ligne) {
        if ( ligne == null ) {
            return null;
        }

        LigneMedicamentEntity ligneMedicamentEntity = new LigneMedicamentEntity();

        ligneMedicamentEntity.setId( ligne.getId() );
        ligneMedicamentEntity.setMedicamentId( ligne.getMedicamentId() );
        ligneMedicamentEntity.setMedicamentNom( ligne.getMedicamentNom() );
        ligneMedicamentEntity.setPrincipeActif( ligne.getPrincipeActif() );
        ligneMedicamentEntity.setDosage( ligne.getDosage() );
        ligneMedicamentEntity.setDureeJours( ligne.getDureeJours() );
        ligneMedicamentEntity.setInstructions( ligne.getInstructions() );
        List<LocalTime> list = ligne.getHeuresPrise();
        if ( list != null ) {
            ligneMedicamentEntity.setHeuresPrise( new ArrayList<LocalTime>( list ) );
        }

        ligneMedicamentEntity.setFrequenceParJour( ligne.getHeuresPrise() != null ? ligne.getHeuresPrise().size() : 0 );

        return ligneMedicamentEntity;
    }

    protected TraitementResponse.LigneResponse ligneMedicamentToLigneResponse(LigneMedicament ligneMedicament) {
        if ( ligneMedicament == null ) {
            return null;
        }

        String medicamentNom = null;
        String principeActif = null;
        String dosage = null;
        int dureeJours = 0;
        List<LocalTime> heuresPrise = null;

        medicamentNom = ligneMedicament.getMedicamentNom();
        principeActif = ligneMedicament.getPrincipeActif();
        dosage = ligneMedicament.getDosage();
        dureeJours = ligneMedicament.getDureeJours();
        List<LocalTime> list = ligneMedicament.getHeuresPrise();
        if ( list != null ) {
            heuresPrise = new ArrayList<LocalTime>( list );
        }

        TraitementResponse.LigneResponse ligneResponse = new TraitementResponse.LigneResponse( medicamentNom, principeActif, dosage, dureeJours, heuresPrise );

        return ligneResponse;
    }

    protected List<TraitementResponse.LigneResponse> ligneMedicamentListToLigneResponseList(List<LigneMedicament> list) {
        if ( list == null ) {
            return null;
        }

        List<TraitementResponse.LigneResponse> list1 = new ArrayList<TraitementResponse.LigneResponse>( list.size() );
        for ( LigneMedicament ligneMedicament : list ) {
            list1.add( ligneMedicamentToLigneResponse( ligneMedicament ) );
        }

        return list1;
    }
}
