package com.pharmaApp.adherence.application.mapper;

import com.pharmaApp.adherence.application.dto.response.AdherenceRecordResponse;
import com.pharmaApp.adherence.application.dto.response.AdherenceSummaryResponse;
import com.pharmaApp.adherence.application.dto.response.HistoriqueEntryResponse;
import com.pharmaApp.adherence.domain.model.AdherenceRecord;
import com.pharmaApp.adherence.domain.model.HistoriqueEntry;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-18T03:56:28+0100",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.17 (Eclipse Adoptium)"
)
@Component
public class AdherenceMapperImpl implements AdherenceMapper {

    @Override
    public AdherenceRecordResponse toResponse(AdherenceRecord record) {
        if ( record == null ) {
            return null;
        }

        AdherenceRecordResponse.AdherenceRecordResponseBuilder adherenceRecordResponse = AdherenceRecordResponse.builder();

        adherenceRecordResponse.id( record.getId() );
        adherenceRecordResponse.patientUserId( record.getPatientUserId() );
        adherenceRecordResponse.traitementId( record.getTraitementId() );
        adherenceRecordResponse.pharmacienUserId( record.getPharmacienUserId() );
        adherenceRecordResponse.taux7j( record.getTaux7j() );
        adherenceRecordResponse.taux30j( record.getTaux30j() );
        adherenceRecordResponse.taux90j( record.getTaux90j() );
        adherenceRecordResponse.tauxGlobal( record.getTauxGlobal() );
        adherenceRecordResponse.consecutiveMissed( record.getConsecutiveMissed() );
        adherenceRecordResponse.lastCalculated( record.getLastCalculated() );
        adherenceRecordResponse.entries( toEntryResponseList( record.getEntries() ) );

        adherenceRecordResponse.totalEntries( record.getEntries() == null ? 0 : record.getEntries().size() );

        return adherenceRecordResponse.build();
    }

    @Override
    public AdherenceSummaryResponse toSummary(AdherenceRecord record) {
        if ( record == null ) {
            return null;
        }

        AdherenceSummaryResponse.AdherenceSummaryResponseBuilder adherenceSummaryResponse = AdherenceSummaryResponse.builder();

        adherenceSummaryResponse.traitementId( record.getTraitementId() );
        adherenceSummaryResponse.patientUserId( record.getPatientUserId() );
        adherenceSummaryResponse.pharmacienUserId( record.getPharmacienUserId() );
        adherenceSummaryResponse.taux7j( record.getTaux7j() );
        adherenceSummaryResponse.taux30j( record.getTaux30j() );
        adherenceSummaryResponse.taux90j( record.getTaux90j() );
        adherenceSummaryResponse.tauxGlobal( record.getTauxGlobal() );
        adherenceSummaryResponse.consecutiveMissed( record.getConsecutiveMissed() );
        adherenceSummaryResponse.lastCalculated( record.getLastCalculated() );

        adherenceSummaryResponse.totalPrises( record.getEntries() == null ? 0 : record.getEntries().size() );
        adherenceSummaryResponse.prisesConfirmees( countByStatut(record, com.pharmaApp.adherence.domain.model.StatutPrise.CONFIRME) );
        adherenceSummaryResponse.prisesManquees( countByStatut(record, com.pharmaApp.adherence.domain.model.StatutPrise.MANQUE) );
        adherenceSummaryResponse.niveauObservance( computeNiveau(record.getTauxGlobal()) );

        return adherenceSummaryResponse.build();
    }

    @Override
    public List<AdherenceSummaryResponse> toSummaryList(List<AdherenceRecord> records) {
        if ( records == null ) {
            return null;
        }

        List<AdherenceSummaryResponse> list = new ArrayList<AdherenceSummaryResponse>( records.size() );
        for ( AdherenceRecord adherenceRecord : records ) {
            list.add( toSummary( adherenceRecord ) );
        }

        return list;
    }

    @Override
    public HistoriqueEntryResponse toEntryResponse(HistoriqueEntry entry) {
        if ( entry == null ) {
            return null;
        }

        HistoriqueEntryResponse.HistoriqueEntryResponseBuilder historiqueEntryResponse = HistoriqueEntryResponse.builder();

        historiqueEntryResponse.id( entry.getId() );
        historiqueEntryResponse.priseMedicamentId( entry.getPriseMedicamentId() );
        historiqueEntryResponse.medicamentNom( entry.getMedicamentNom() );
        historiqueEntryResponse.dosage( entry.getDosage() );
        historiqueEntryResponse.datePrise( entry.getDatePrise() );
        historiqueEntryResponse.heurePrise( entry.getHeurePrise() );
        historiqueEntryResponse.heureConfirmation( entry.getHeureConfirmation() );
        historiqueEntryResponse.delaiMinutes( entry.getDelaiMinutes() );
        historiqueEntryResponse.notePatient( entry.getNotePatient() );

        historiqueEntryResponse.statut( entry.getStatut().name() );

        return historiqueEntryResponse.build();
    }

    @Override
    public List<HistoriqueEntryResponse> toEntryResponseList(List<HistoriqueEntry> entries) {
        if ( entries == null ) {
            return null;
        }

        List<HistoriqueEntryResponse> list = new ArrayList<HistoriqueEntryResponse>( entries.size() );
        for ( HistoriqueEntry historiqueEntry : entries ) {
            list.add( toEntryResponse( historiqueEntry ) );
        }

        return list;
    }
}
