package com.pharmaApp.adherence.infrastructure.adapter.output.persistence.mapper;

import com.pharmaApp.adherence.domain.model.AdherenceRecord;
import com.pharmaApp.adherence.domain.model.HistoriqueEntry;
import com.pharmaApp.adherence.infrastructure.adapter.output.persistence.entity.AdherenceRecordEntity;
import com.pharmaApp.adherence.infrastructure.adapter.output.persistence.entity.HistoriqueEntryEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-18T18:41:29+0100",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.17 (Eclipse Adoptium)"
)
@Component
public class AdherenceEntityMapperImpl implements AdherenceEntityMapper {

    @Override
    public AdherenceRecord toDomain(AdherenceRecordEntity entity) {
        if ( entity == null ) {
            return null;
        }

        AdherenceRecord.AdherenceRecordBuilder adherenceRecord = AdherenceRecord.builder();

        adherenceRecord.entries( historiqueEntryEntityListToHistoriqueEntryList( entity.getEntries() ) );
        adherenceRecord.id( entity.getId() );
        adherenceRecord.patientUserId( entity.getPatientUserId() );
        adherenceRecord.traitementId( entity.getTraitementId() );
        adherenceRecord.pharmacienUserId( entity.getPharmacienUserId() );
        adherenceRecord.taux7j( entity.getTaux7j() );
        adherenceRecord.taux30j( entity.getTaux30j() );
        adherenceRecord.taux90j( entity.getTaux90j() );
        adherenceRecord.tauxGlobal( entity.getTauxGlobal() );
        adherenceRecord.consecutiveMissed( entity.getConsecutiveMissed() );
        adherenceRecord.lastCalculated( entity.getLastCalculated() );

        return adherenceRecord.build();
    }

    @Override
    public List<AdherenceRecord> toDomainList(List<AdherenceRecordEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<AdherenceRecord> list = new ArrayList<AdherenceRecord>( entities.size() );
        for ( AdherenceRecordEntity adherenceRecordEntity : entities ) {
            list.add( toDomain( adherenceRecordEntity ) );
        }

        return list;
    }

    @Override
    public HistoriqueEntry toDomain(HistoriqueEntryEntity entity) {
        if ( entity == null ) {
            return null;
        }

        HistoriqueEntry.HistoriqueEntryBuilder historiqueEntry = HistoriqueEntry.builder();

        historiqueEntry.adherenceRecordId( entityAdherenceRecordId( entity ) );
        historiqueEntry.id( entity.getId() );
        historiqueEntry.priseMedicamentId( entity.getPriseMedicamentId() );
        historiqueEntry.medicamentNom( entity.getMedicamentNom() );
        historiqueEntry.dosage( entity.getDosage() );
        historiqueEntry.datePrise( entity.getDatePrise() );
        historiqueEntry.heurePrise( entity.getHeurePrise() );
        historiqueEntry.heureConfirmation( entity.getHeureConfirmation() );
        historiqueEntry.delaiMinutes( entity.getDelaiMinutes() );
        historiqueEntry.notePatient( entity.getNotePatient() );

        historiqueEntry.statut( normalizeStatut(entity.getStatut()) );

        return historiqueEntry.build();
    }

    @Override
    public AdherenceRecordEntity toEntity(AdherenceRecord domain) {
        if ( domain == null ) {
            return null;
        }

        AdherenceRecordEntity.AdherenceRecordEntityBuilder adherenceRecordEntity = AdherenceRecordEntity.builder();

        adherenceRecordEntity.id( domain.getId() );
        adherenceRecordEntity.patientUserId( domain.getPatientUserId() );
        adherenceRecordEntity.traitementId( domain.getTraitementId() );
        adherenceRecordEntity.pharmacienUserId( domain.getPharmacienUserId() );
        adherenceRecordEntity.taux7j( domain.getTaux7j() );
        adherenceRecordEntity.taux30j( domain.getTaux30j() );
        adherenceRecordEntity.taux90j( domain.getTaux90j() );
        adherenceRecordEntity.tauxGlobal( domain.getTauxGlobal() );
        adherenceRecordEntity.consecutiveMissed( domain.getConsecutiveMissed() );
        adherenceRecordEntity.lastCalculated( domain.getLastCalculated() );

        return adherenceRecordEntity.build();
    }

    @Override
    public HistoriqueEntryEntity toEntity(HistoriqueEntry domain) {
        if ( domain == null ) {
            return null;
        }

        HistoriqueEntryEntity.HistoriqueEntryEntityBuilder historiqueEntryEntity = HistoriqueEntryEntity.builder();

        historiqueEntryEntity.id( domain.getId() );
        historiqueEntryEntity.priseMedicamentId( domain.getPriseMedicamentId() );
        historiqueEntryEntity.medicamentNom( domain.getMedicamentNom() );
        historiqueEntryEntity.dosage( domain.getDosage() );
        historiqueEntryEntity.datePrise( domain.getDatePrise() );
        historiqueEntryEntity.heurePrise( domain.getHeurePrise() );
        historiqueEntryEntity.heureConfirmation( domain.getHeureConfirmation() );
        historiqueEntryEntity.delaiMinutes( domain.getDelaiMinutes() );
        historiqueEntryEntity.notePatient( domain.getNotePatient() );

        historiqueEntryEntity.statut( domain.getStatut().name() );

        return historiqueEntryEntity.build();
    }

    @Override
    public List<HistoriqueEntryEntity> toEntityList(List<HistoriqueEntry> domains) {
        if ( domains == null ) {
            return null;
        }

        List<HistoriqueEntryEntity> list = new ArrayList<HistoriqueEntryEntity>( domains.size() );
        for ( HistoriqueEntry historiqueEntry : domains ) {
            list.add( toEntity( historiqueEntry ) );
        }

        return list;
    }

    protected List<HistoriqueEntry> historiqueEntryEntityListToHistoriqueEntryList(List<HistoriqueEntryEntity> list) {
        if ( list == null ) {
            return null;
        }

        List<HistoriqueEntry> list1 = new ArrayList<HistoriqueEntry>( list.size() );
        for ( HistoriqueEntryEntity historiqueEntryEntity : list ) {
            list1.add( toDomain( historiqueEntryEntity ) );
        }

        return list1;
    }

    private Long entityAdherenceRecordId(HistoriqueEntryEntity historiqueEntryEntity) {
        AdherenceRecordEntity adherenceRecord = historiqueEntryEntity.getAdherenceRecord();
        if ( adherenceRecord == null ) {
            return null;
        }
        return adherenceRecord.getId();
    }
}
