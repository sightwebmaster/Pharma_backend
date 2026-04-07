package com.pharmaApp.adherence.application.mapper;

import com.pharmaApp.adherence.application.dto.response.*;
import com.pharmaApp.adherence.domain.model.*;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdherenceMapper {

    // ── AdherenceRecord → Response ────────────────────────────

    @Mapping(target = "totalEntries", expression = "java(record.getEntries() == null ? 0 : record.getEntries().size())")
    AdherenceRecordResponse toResponse(AdherenceRecord record);

    // ── AdherenceRecord → Summary ─────────────────────────────

    @Mapping(target = "totalPrises",      expression = "java(record.getEntries() == null ? 0 : record.getEntries().size())")
    @Mapping(target = "prisesConfirmees", expression = "java(countByStatut(record, com.pharmaApp.adherence.domain.model.StatutPrise.CONFIRME))")
    @Mapping(target = "prisesManquees",   expression = "java(countByStatut(record, com.pharmaApp.adherence.domain.model.StatutPrise.MANQUE))")
    @Mapping(target = "niveauObservance", expression = "java(computeNiveau(record.getTauxGlobal()))")
    @Mapping(target = "alerteEnvoyee",    ignore = true)
    AdherenceSummaryResponse toSummary(AdherenceRecord record);

    List<AdherenceSummaryResponse> toSummaryList(List<AdherenceRecord> records);

    // ── HistoriqueEntry → Response ────────────────────────────

    @Mapping(target = "statut", expression = "java(entry.getStatut().name())")
    HistoriqueEntryResponse toEntryResponse(HistoriqueEntry entry);

    List<HistoriqueEntryResponse> toEntryResponseList(List<HistoriqueEntry> entries);

    // ── Helpers ───────────────────────────────────────────────

    default int countByStatut(AdherenceRecord record, StatutPrise statut) {
        if (record.getEntries() == null) return 0;
        return (int) record.getEntries().stream()
                .filter(e -> e.getStatut() == statut)
                .count();
    }

    default String computeNiveau(double taux) {
        if (taux >= 80) return "BON";
        if (taux >= 70) return "MOYEN";
        return "CRITIQUE";
    }
}
