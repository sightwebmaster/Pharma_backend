package com.pharmaApp.user.infrastructure.adapter.output.persistence.mapper;

import com.pharmaApp.user.domain.model.PharmacienProfile;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.entity.PharmacienProfileEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-18T03:23:33+0100",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.17 (Eclipse Adoptium)"
)
@Component
public class PharmacienProfileEntityMapperImpl implements PharmacienProfileEntityMapper {

    @Override
    public PharmacienProfile toDomain(PharmacienProfileEntity entity) {
        if ( entity == null ) {
            return null;
        }

        PharmacienProfile.PharmacienProfileBuilder pharmacienProfile = PharmacienProfile.builder();

        pharmacienProfile.id( entity.getId() );
        pharmacienProfile.userId( entity.getUserId() );
        pharmacienProfile.nom( entity.getNom() );
        pharmacienProfile.prenom( entity.getPrenom() );
        pharmacienProfile.email( entity.getEmail() );
        pharmacienProfile.telephone( entity.getTelephone() );
        pharmacienProfile.dateNaissance( entity.getDateNaissance() );
        pharmacienProfile.numeroOrdre( entity.getNumeroOrdre() );
        pharmacienProfile.specialite( entity.getSpecialite() );
        pharmacienProfile.createdAt( entity.getCreatedAt() );
        pharmacienProfile.updatedAt( entity.getUpdatedAt() );
        pharmacienProfile.photoBase64( entity.getPhotoBase64() );
        pharmacienProfile.qrCode( entity.getQrCode() );

        return pharmacienProfile.build();
    }

    @Override
    public PharmacienProfileEntity toEntity(PharmacienProfile domain) {
        if ( domain == null ) {
            return null;
        }

        PharmacienProfileEntity.PharmacienProfileEntityBuilder pharmacienProfileEntity = PharmacienProfileEntity.builder();

        pharmacienProfileEntity.id( domain.getId() );
        pharmacienProfileEntity.userId( domain.getUserId() );
        pharmacienProfileEntity.nom( domain.getNom() );
        pharmacienProfileEntity.prenom( domain.getPrenom() );
        pharmacienProfileEntity.email( domain.getEmail() );
        pharmacienProfileEntity.telephone( domain.getTelephone() );
        pharmacienProfileEntity.dateNaissance( domain.getDateNaissance() );
        pharmacienProfileEntity.numeroOrdre( domain.getNumeroOrdre() );
        pharmacienProfileEntity.specialite( domain.getSpecialite() );
        pharmacienProfileEntity.photoBase64( domain.getPhotoBase64() );
        pharmacienProfileEntity.qrCode( domain.getQrCode() );
        pharmacienProfileEntity.createdAt( domain.getCreatedAt() );
        pharmacienProfileEntity.updatedAt( domain.getUpdatedAt() );

        return pharmacienProfileEntity.build();
    }
}
