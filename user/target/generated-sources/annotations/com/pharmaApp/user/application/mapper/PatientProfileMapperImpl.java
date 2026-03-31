package com.pharmaApp.user.application.mapper;

import com.pharmaApp.user.application.dto.request.CreatePatientProfileRequest;
import com.pharmaApp.user.application.dto.request.RegisterPatientRequest;
import com.pharmaApp.user.application.dto.request.UpdatePatientProfileRequest;
import com.pharmaApp.user.application.dto.response.PatientProfileResponse;
import com.pharmaApp.user.domain.model.PatientProfile;
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
public class PatientProfileMapperImpl implements PatientProfileMapper {

    @Override
    public PatientProfile toDomain(CreatePatientProfileRequest request) {
        if ( request == null ) {
            return null;
        }

        PatientProfile.PatientProfileBuilder patientProfile = PatientProfile.builder();

        patientProfile.nom( request.getNom() );
        patientProfile.prenom( request.getPrenom() );
        patientProfile.email( request.getEmail() );
        patientProfile.telephone( request.getTelephone() );
        patientProfile.dateNaissance( request.getDateNaissance() );
        patientProfile.groupeSanguin( request.getGroupeSanguin() );
        List<String> list = request.getAllergies();
        if ( list != null ) {
            patientProfile.allergies( new ArrayList<String>( list ) );
        }
        List<String> list1 = request.getMaladiesChroniques();
        if ( list1 != null ) {
            patientProfile.maladiesChroniques( new ArrayList<String>( list1 ) );
        }

        return patientProfile.build();
    }

    @Override
    public PatientProfile toDomain(RegisterPatientRequest request) {
        if ( request == null ) {
            return null;
        }

        PatientProfile.PatientProfileBuilder patientProfile = PatientProfile.builder();

        patientProfile.nom( request.getNom() );
        patientProfile.prenom( request.getPrenom() );
        patientProfile.email( request.getEmail() );
        patientProfile.telephone( request.getTelephone() );
        patientProfile.dateNaissance( request.getDateNaissance() );
        patientProfile.groupeSanguin( request.getGroupeSanguin() );
        List<String> list = request.getAllergies();
        if ( list != null ) {
            patientProfile.allergies( new ArrayList<String>( list ) );
        }
        List<String> list1 = request.getMaladiesChroniques();
        if ( list1 != null ) {
            patientProfile.maladiesChroniques( new ArrayList<String>( list1 ) );
        }

        return patientProfile.build();
    }

    @Override
    public PatientProfileResponse toResponse(PatientProfile domain) {
        if ( domain == null ) {
            return null;
        }

        PatientProfileResponse patientProfileResponse = new PatientProfileResponse();

        patientProfileResponse.setId( domain.getId() );
        patientProfileResponse.setUserId( domain.getUserId() );
        patientProfileResponse.setNom( domain.getNom() );
        patientProfileResponse.setPrenom( domain.getPrenom() );
        patientProfileResponse.setDateNaissance( domain.getDateNaissance() );
        patientProfileResponse.setTelephone( domain.getTelephone() );
        patientProfileResponse.setGroupeSanguin( domain.getGroupeSanguin() );
        List<String> list = domain.getAllergies();
        if ( list != null ) {
            patientProfileResponse.setAllergies( new ArrayList<String>( list ) );
        }
        List<String> list1 = domain.getMaladiesChroniques();
        if ( list1 != null ) {
            patientProfileResponse.setMaladiesChroniques( new ArrayList<String>( list1 ) );
        }
        patientProfileResponse.setCreatedAt( domain.getCreatedAt() );
        patientProfileResponse.setQrCode( domain.getQrCode() );

        return patientProfileResponse;
    }

    @Override
    public void updateDomainFromRequest(UpdatePatientProfileRequest request, PatientProfile domain) {
        if ( request == null ) {
            return;
        }

        if ( request.getNom() != null ) {
            domain.setNom( request.getNom() );
        }
        if ( request.getPrenom() != null ) {
            domain.setPrenom( request.getPrenom() );
        }
        if ( request.getTelephone() != null ) {
            domain.setTelephone( request.getTelephone() );
        }
        if ( request.getDateNaissance() != null ) {
            domain.setDateNaissance( request.getDateNaissance() );
        }
        if ( request.getGroupeSanguin() != null ) {
            domain.setGroupeSanguin( request.getGroupeSanguin() );
        }
        if ( domain.getAllergies() != null ) {
            List<String> list = request.getAllergies();
            if ( list != null ) {
                domain.getAllergies().clear();
                domain.getAllergies().addAll( list );
            }
        }
        else {
            List<String> list = request.getAllergies();
            if ( list != null ) {
                domain.setAllergies( new ArrayList<String>( list ) );
            }
        }
        if ( domain.getMaladiesChroniques() != null ) {
            List<String> list1 = request.getMaladiesChroniques();
            if ( list1 != null ) {
                domain.getMaladiesChroniques().clear();
                domain.getMaladiesChroniques().addAll( list1 );
            }
        }
        else {
            List<String> list1 = request.getMaladiesChroniques();
            if ( list1 != null ) {
                domain.setMaladiesChroniques( new ArrayList<String>( list1 ) );
            }
        }
    }
}
