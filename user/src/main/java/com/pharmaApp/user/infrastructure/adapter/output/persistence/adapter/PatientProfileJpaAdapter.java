package com.pharmaApp.user.infrastructure.adapter.output.persistence.adapter;

import com.pharmaApp.user.domain.model.PatientProfile;
import com.pharmaApp.user.domain.port.output.PatientProfileRepositoryPort;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.mapper.PatientProfileEntityMapper;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.repository.PatientProfileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PatientProfileJpaAdapter implements PatientProfileRepositoryPort {

    private final PatientProfileJpaRepository repo;
    private final PatientProfileEntityMapper mapper;

    @Override
    public PatientProfile save(PatientProfile domain) {
        return mapper.toDomain(repo.save(mapper.toEntity(domain)));
    }

    @Override
    public Optional<PatientProfile> findByUserId(String userId) {
        return repo.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public Optional<PatientProfile> findByEmail(String email) { // ✅
        return repo.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUserId(String userId) {
        return repo.existsByUserId(userId);
    }

    @Override
    public void deleteByUserId(String userId) {
        repo.deleteByUserId(userId);
    }
}