// PharmacienProfileJpaAdapter.java
package com.pharmaApp.user.infrastructure.adapter.output.persistence.adapter;

import com.pharmaApp.user.domain.model.PharmacienProfile;
import com.pharmaApp.user.domain.port.output.PharmacienProfileRepositoryPort;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.mapper.PharmacienProfileEntityMapper;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.repository.PharmacienProfileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PharmacienProfileJpaAdapter implements PharmacienProfileRepositoryPort {

    private final PharmacienProfileJpaRepository repo;
    private final PharmacienProfileEntityMapper mapper;

    @Override
    public PharmacienProfile save(PharmacienProfile domain) {
        return mapper.toDomain(
                repo.save(mapper.toEntity(domain))
        );
    }

    @Override
    public Optional<PharmacienProfile> findByUserId(String userId) {
        return repo.findByUserId(userId)
                .map(mapper::toDomain);
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