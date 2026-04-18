package com.pharmaApp.user.application.mapper;

import com.pharmaApp.user.application.dto.response.PharmacienProfileResponse;
import com.pharmaApp.user.domain.model.PharmacienProfile;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-18T17:49:58+0100",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.17 (Eclipse Adoptium)"
)
@Component
public class PharmacienProfileMapperImpl implements PharmacienProfileMapper {

    @Override
    public PharmacienProfileResponse toResponse(PharmacienProfile domain) {
        if ( domain == null ) {
            return null;
        }

        PharmacienProfileResponse pharmacienProfileResponse = new PharmacienProfileResponse();

        pharmacienProfileResponse.setId( domain.getId() );
        pharmacienProfileResponse.setUserId( domain.getUserId() );
        pharmacienProfileResponse.setNom( domain.getNom() );
        pharmacienProfileResponse.setPrenom( domain.getPrenom() );
        pharmacienProfileResponse.setTelephone( domain.getTelephone() );
        pharmacienProfileResponse.setEmail( domain.getEmail() );
        pharmacienProfileResponse.setDateNaissance( domain.getDateNaissance() );
        pharmacienProfileResponse.setNumeroOrdre( domain.getNumeroOrdre() );
        pharmacienProfileResponse.setSpecialite( domain.getSpecialite() );
        pharmacienProfileResponse.setCreatedAt( domain.getCreatedAt() );
        pharmacienProfileResponse.setPhotoBase64( domain.getPhotoBase64() );
        pharmacienProfileResponse.setQrCode( domain.getQrCode() );

        return pharmacienProfileResponse;
    }
}
