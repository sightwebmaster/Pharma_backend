package com.pharmaApp.user.application.mapper;

import com.pharmaApp.user.application.dto.request.CreatePatientProfileRequest;
import com.pharmaApp.user.application.dto.request.RegisterPatientRequest;
import com.pharmaApp.user.application.dto.request.UpdatePatientProfileRequest;
import com.pharmaApp.user.application.dto.response.PatientProfileResponse;
import com.pharmaApp.user.domain.model.PatientProfile;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PatientProfileMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "qrCode", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "photoBase64", ignore = true)
    PatientProfile toDomain(CreatePatientProfileRequest request);

    // ✅ Nouveau — pour AuthService.register()
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "qrCode", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "photoBase64", ignore = true)
    PatientProfile toDomain(RegisterPatientRequest request);


    PatientProfileResponse toResponse(PatientProfile domain);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "qrCode", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "email", ignore = true)   // ✅ ajouter

    void updateDomainFromRequest(UpdatePatientProfileRequest request,
                                 @MappingTarget PatientProfile domain);
}
