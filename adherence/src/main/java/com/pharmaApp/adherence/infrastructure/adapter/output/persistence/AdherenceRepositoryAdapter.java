package com.pharmaApp.adherence.infrastructure.adapter.output.persistence;

import com.pharmaApp.adherence.domain.model.*;
import com.pharmaApp.adherence.domain.port.output.AdherenceRepository;
import com.pharmaApp.adherence.infrastructure.adapter.output.persistence.entity.*;
import com.pharmaApp.adherence.infrastructure.adapter.output.persistence.mapper.AdherenceEntityMapper;
import com.pharmaApp.adherence.infrastructure.adapter.output.persistence.repository.AdherenceRecordJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdherenceRepositoryAdapter implements AdherenceRepository {

    private final AdherenceRecordJpaRepository jpaRepository;
    private final AdherenceEntityMapper         mapper;

    @Override
    public AdherenceRecord save(AdherenceRecord record) {
        AdherenceRecordEntity entity;

        if (record.getId() != null) {
            entity = jpaRepository.findById(record.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "AdherenceRecord introuvable id=" + record.getId()));
        } else {
            entity = new AdherenceRecordEntity();
        }

        entity.setPatientUserId(record.getPatientUserId());
        entity.setTraitementId(record.getTraitementId());       // ✅ String
        entity.setPharmacienUserId(record.getPharmacienUserId());
        entity.setTaux7j(record.getTaux7j());
        entity.setTaux30j(record.getTaux30j());
        entity.setTaux90j(record.getTaux90j());
        entity.setTauxGlobal(record.getTauxGlobal());
        entity.setConsecutiveMissed(record.getConsecutiveMissed());
        entity.setLastCalculated(record.getLastCalculated());

        if (record.getEntries() != null) {
            entity.getEntries().removeIf(e -> record.getEntries().stream()
                    .noneMatch(d -> d.getId() != null && d.getId().equals(e.getId())));

            for (HistoriqueEntry domainEntry : record.getEntries()) {
                if (domainEntry.getId() == null) {
                    HistoriqueEntryEntity entryEntity = mapper.toEntity(domainEntry);
                    entryEntity.setAdherenceRecord(entity);
                    entity.getEntries().add(entryEntity);
                }
            }
        }

        AdherenceRecordEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AdherenceRecord> findByPatientAndTraitement(
            String patientUserId, String traitementId) {   // ✅ String
        return jpaRepository
                .findByPatientUserIdAndTraitementId(patientUserId, traitementId)
                .map(mapper::toDomain);
    }

    @Override
    public List<AdherenceRecord> findAllByPatient(String patientUserId) {
        return mapper.toDomainList(jpaRepository.findAllByPatientUserId(patientUserId));
    }

    @Override
    public List<AdherenceRecord> findAllByPharmacien(String pharmacienUserId) {
        return mapper.toDomainList(jpaRepository.findAllByPharmacienUserId(pharmacienUserId));
    }

    @Override
    public boolean existsByPatientAndTraitement(
            String patientUserId, String traitementId) {   // ✅ String
        return jpaRepository.existsByPatientUserIdAndTraitementId(
                patientUserId, traitementId);
    }
}