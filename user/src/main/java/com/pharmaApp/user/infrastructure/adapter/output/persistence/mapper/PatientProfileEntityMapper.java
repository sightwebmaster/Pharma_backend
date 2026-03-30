package com.pharmaApp.user.infrastructure.adapter.output.persistence.mapper;

import com.pharmaApp.user.domain.model.PatientProfile;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.entity.PatientProfileEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PatientProfileEntityMapper {

    // Entity → Domain (après lecture BDD)
    PatientProfile toDomain(PatientProfileEntity entity);

    // Domain → Entity (avant sauvegarde BDD)
    PatientProfileEntity toEntity(PatientProfile domain);
}