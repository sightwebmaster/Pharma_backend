package com.pharmaApp.user.infrastructure.adapter.output.persistence.adapter;

import com.pharmaApp.user.domain.model.Proche;
import com.pharmaApp.user.domain.port.output.ProcheRepositoryPort;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.mapper.ProcheEntityMapper;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.repository.ProcheJpaRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class ProcheJpaAdapter implements ProcheRepositoryPort {

    private final ProcheJpaRepository repo;
    private final ProcheEntityMapper mapper;

    public ProcheJpaAdapter(ProcheJpaRepository repo, ProcheEntityMapper mapper) {
        this.repo   = repo;
        this.mapper = mapper;
    }

    @Override
    public Proche save(Proche domain) {
        return mapper.toDomain(repo.save(mapper.toEntity(domain)));
    }

    @Override
    public Optional<Proche> findById(String id) {
        return repo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Proche> findAllByPatientUserId(String patientUserId) {
        return mapper.toDomainList(repo.findByPatientUserId(patientUserId));
    }

    @Override
    public boolean existsByIdAndPatientUserId(String id, String patientUserId) {
        return repo.existsByIdAndPatientUserId(id, patientUserId);
    }

    @Override
    public boolean existsByPatientUserIdAndProcheUserId(
            String patientUserId, String procheUserId) {
        return repo.existsByPatientUserIdAndProcheUserId(patientUserId, procheUserId);
    }

    @Override
    public void deleteById(String id) {
        repo.deleteById(id);
    }
}