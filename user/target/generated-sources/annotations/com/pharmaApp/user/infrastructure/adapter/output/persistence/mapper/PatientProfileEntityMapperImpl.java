package com.pharmaApp.user.infrastructure.adapter.output.persistence.mapper;

import com.pharmaApp.user.domain.model.PatientProfile;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.entity.PatientProfileEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-30T20:33:50+0100",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.18 (BellSoft)"
)
@Component
public class PatientProfileEntityMapperImpl implements PatientProfileEntityMapper {

    @Override
    public PatientProfile toDomain(PatientProfileEntity entity) {
        if ( entity == null ) {
            return null;
        }

        PatientProfile.PatientProfileBuilder patientProfile = PatientProfile.builder();

        patientProfile.id( entity.getId() );
        patientProfile.userId( entity.getUserId() );
        patientProfile.nom( entity.getNom() );
        patientProfile.prenom( entity.getPrenom() );
        patientProfile.email( entity.getEmail() );
        patientProfile.telephone( entity.getTelephone() );
        patientProfile.dateNaissance( entity.getDateNaissance() );
        patientProfile.groupeSanguin( entity.getGroupeSanguin() );
        List<String> list = entity.getAllergies();
        if ( list != null ) {
            patientProfile.allergies( new ArrayList<String>( list ) );
        }
        List<String> list1 = entity.getMaladiesChroniques();
        if ( list1 != null ) {
            patientProfile.maladiesChroniques( new ArrayList<String>( list1 ) );
        }
        patientProfile.createdAt( entity.getCreatedAt() );
        patientProfile.updatedAt( entity.getUpdatedAt() );
        patientProfile.qrCode( entity.getQrCode() );

        return patientProfile.build();
    }

    @Override
    public PatientProfileEntity toEntity(PatientProfile domain) {
        if ( domain == null ) {
            return null;
        }

        PatientProfileEntity.PatientProfileEntityBuilder patientProfileEntity = PatientProfileEntity.builder();

        patientProfileEntity.id( domain.getId() );
        patientProfileEntity.userId( domain.getUserId() );
        patientProfileEntity.email( domain.getEmail() );
        patientProfileEntity.nom( domain.getNom() );
        patientProfileEntity.prenom( domain.getPrenom() );
        patientProfileEntity.telephone( domain.getTelephone() );
        patientProfileEntity.dateNaissance( domain.getDateNaissance() );
        patientProfileEntity.groupeSanguin( domain.getGroupeSanguin() );
        List<String> list = domain.getAllergies();
        if ( list != null ) {
            patientProfileEntity.allergies( new ArrayList<String>( list ) );
        }
        List<String> list1 = domain.getMaladiesChroniques();
        if ( list1 != null ) {
            patientProfileEntity.maladiesChroniques( new ArrayList<String>( list1 ) );
        }
        patientProfileEntity.qrCode( domain.getQrCode() );
        patientProfileEntity.createdAt( domain.getCreatedAt() );
        patientProfileEntity.updatedAt( domain.getUpdatedAt() );

        return patientProfileEntity.build();
    }
}
