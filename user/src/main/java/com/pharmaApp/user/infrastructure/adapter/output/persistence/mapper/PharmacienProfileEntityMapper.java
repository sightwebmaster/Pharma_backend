package com.pharmaApp.user.infrastructure.adapter.output.persistence.mapper;

import com.pharmaApp.user.domain.model.PharmacienProfile;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.entity.PharmacienProfileEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PharmacienProfileEntityMapper {

    PharmacienProfile toDomain(PharmacienProfileEntity entity);

    PharmacienProfileEntity toEntity(PharmacienProfile domain);
}
