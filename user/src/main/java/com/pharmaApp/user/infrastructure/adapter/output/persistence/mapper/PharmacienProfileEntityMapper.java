package com.pharmaApp.user.infrastructure.adapter.output.persistence.mapper;

import com.pharmaApp.user.domain.model.PharmacienProfile;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.entity.PharmacienProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PharmacienProfileEntityMapper {

    // Entity → Domain : email absent dans entity → ignorer
    @Mapping(target = "email", ignore = true)   // ✅ si email absent dans Entity
    PharmacienProfile toDomain(PharmacienProfileEntity entity);

    // Domain → Entity : email absent dans entity → ignorer
    @Mapping(target = "email", ignore = true)   // ✅ si email absent dans Entity
    PharmacienProfileEntity toEntity(PharmacienProfile domain);
}