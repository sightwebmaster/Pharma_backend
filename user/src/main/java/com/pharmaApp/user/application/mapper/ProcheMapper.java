package com.pharmaApp.user.application.mapper;

import com.pharmaApp.user.application.dto.response.ProcheResponse;
import com.pharmaApp.user.domain.model.Proche;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProcheMapper {

    // ✅ nom/prenom/etc viennent de PatientProfile — pas de Proche
    @Mapping(target = "nom", ignore = true)
    @Mapping(target = "prenom", ignore = true)
    @Mapping(target = "telephone", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "groupeSanguin", ignore = true)
    ProcheResponse toResponse(Proche proche);

    List<ProcheResponse> toResponseList(List<Proche> proches);
}