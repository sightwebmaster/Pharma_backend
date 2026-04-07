package com.pharmaApp.user.infrastructure.adapter.output.persistence.mapper;

import com.pharmaApp.user.domain.model.Proche;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.entity.ProcheEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-03T19:23:10+0100",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.18 (BellSoft)"
)
@Component
public class ProcheEntityMapperImpl implements ProcheEntityMapper {

    @Override
    public Proche toDomain(ProcheEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Proche.ProcheBuilder proche = Proche.builder();

        proche.id( entity.getId() );
        proche.patientUserId( entity.getPatientUserId() );
        proche.procheUserId( entity.getProcheUserId() );
        proche.relation( entity.getRelation() );

        return proche.build();
    }

    @Override
    public ProcheEntity toEntity(Proche domain) {
        if ( domain == null ) {
            return null;
        }

        ProcheEntity.ProcheEntityBuilder procheEntity = ProcheEntity.builder();

        procheEntity.id( domain.getId() );
        procheEntity.patientUserId( domain.getPatientUserId() );
        procheEntity.procheUserId( domain.getProcheUserId() );
        procheEntity.relation( domain.getRelation() );

        return procheEntity.build();
    }

    @Override
    public List<Proche> toDomainList(List<ProcheEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<Proche> list = new ArrayList<Proche>( entities.size() );
        for ( ProcheEntity procheEntity : entities ) {
            list.add( toDomain( procheEntity ) );
        }

        return list;
    }
}
