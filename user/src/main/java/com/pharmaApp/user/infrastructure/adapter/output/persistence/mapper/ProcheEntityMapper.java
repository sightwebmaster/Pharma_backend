package com.pharmaApp.user.infrastructure.adapter.output.persistence.mapper;

import com.pharmaApp.user.domain.model.Proche;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.entity.ProcheEntity;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProcheEntityMapper {
    Proche toDomain(ProcheEntity entity);
    ProcheEntity toEntity(Proche domain);
    List<Proche> toDomainList(List<ProcheEntity> entities);
}