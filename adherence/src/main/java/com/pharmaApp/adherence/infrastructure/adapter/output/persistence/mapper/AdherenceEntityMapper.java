package com.pharmaApp.adherence.infrastructure.adapter.output.persistence.mapper;

import com.pharmaApp.adherence.domain.model.*;
import com.pharmaApp.adherence.infrastructure.adapter.output.persistence.entity.*;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdherenceEntityMapper {

    // ── Entity → Domain ────────────────────────────────────────

    @Mapping(target = "entries", source = "entries")
    AdherenceRecord toDomain(AdherenceRecordEntity entity);

    List<AdherenceRecord> toDomainList(List<AdherenceRecordEntity> entities);

    @Mapping(target = "adherenceRecordId", source = "adherenceRecord.id")
    @Mapping(target = "statut",            expression = "java(normalizeStatut(entity.getStatut()))")
    HistoriqueEntry toDomain(HistoriqueEntryEntity entity);

    // ── Domain → Entity ────────────────────────────────────────

    @Mapping(target = "entries", ignore = true)  // géré manuellement pour éviter les cycles
    AdherenceRecordEntity toEntity(AdherenceRecord domain);

    @Mapping(target = "adherenceRecord", ignore = true) // setté manuellement
    @Mapping(target = "statut",          expression = "java(domain.getStatut().name())")
    HistoriqueEntryEntity toEntity(HistoriqueEntry domain);

    List<HistoriqueEntryEntity> toEntityList(List<HistoriqueEntry> domains);

    default StatutPrise normalizeStatut(String rawStatut) {
        if (rawStatut == null || rawStatut.isBlank()) {
            return StatutPrise.MANQUE;
        }

        return switch (rawStatut.trim().toUpperCase()) {
            case "CONFIRME", "CONFIRMEE" -> StatutPrise.CONFIRME;
            case "MANQUE", "MANQUEE" -> StatutPrise.MANQUE;
            default -> StatutPrise.valueOf(rawStatut.trim().toUpperCase());
        };
    }
}
