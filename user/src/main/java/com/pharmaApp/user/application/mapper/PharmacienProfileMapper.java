package com.pharmaApp.user.application.mapper;

import com.pharmaApp.user.application.dto.response.PharmacienProfileResponse;
import com.pharmaApp.user.domain.model.PharmacienProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PharmacienProfileMapper {

    // Domain → Response (lecture seule côté pharmacien)
    PharmacienProfileResponse toResponse(PharmacienProfile domain);
}