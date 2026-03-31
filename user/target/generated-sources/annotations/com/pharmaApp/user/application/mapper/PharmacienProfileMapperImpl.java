package com.pharmaApp.user.application.mapper;

import com.pharmaApp.user.application.dto.response.PharmacienProfileResponse;
import com.pharmaApp.user.domain.model.PharmacienProfile;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-30T20:33:50+0100",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.18 (BellSoft)"
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
        pharmacienProfileResponse.setNumeroOrdre( domain.getNumeroOrdre() );
        pharmacienProfileResponse.setSpecialite( domain.getSpecialite() );
        pharmacienProfileResponse.setCreatedAt( domain.getCreatedAt() );

        return pharmacienProfileResponse;
    }
}
