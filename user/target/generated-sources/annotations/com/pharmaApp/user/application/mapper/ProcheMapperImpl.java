package com.pharmaApp.user.application.mapper;

import com.pharmaApp.user.application.dto.response.ProcheResponse;
import com.pharmaApp.user.domain.model.Proche;
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
public class ProcheMapperImpl implements ProcheMapper {

    @Override
    public ProcheResponse toResponse(Proche proche) {
        if ( proche == null ) {
            return null;
        }

        ProcheResponse.ProcheResponseBuilder procheResponse = ProcheResponse.builder();

        procheResponse.id( proche.getId() );
        procheResponse.patientUserId( proche.getPatientUserId() );
        procheResponse.procheUserId( proche.getProcheUserId() );
        procheResponse.relation( proche.getRelation() );

        return procheResponse.build();
    }

    @Override
    public List<ProcheResponse> toResponseList(List<Proche> proches) {
        if ( proches == null ) {
            return null;
        }

        List<ProcheResponse> list = new ArrayList<ProcheResponse>( proches.size() );
        for ( Proche proche : proches ) {
            list.add( toResponse( proche ) );
        }

        return list;
    }
}
