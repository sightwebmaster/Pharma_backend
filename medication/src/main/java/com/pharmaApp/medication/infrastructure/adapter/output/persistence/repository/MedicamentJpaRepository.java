package com.pharmaApp.medication.infrastructure.adapter.output.persistence.repository;

import com.pharmaApp.medication.infrastructure.adapter.output.persistence.entity.MedicamentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicamentJpaRepository
        extends JpaRepository<MedicamentEntity, String> {

    /** Recherche par nom (insensible à la casse) */
    List<MedicamentEntity> findByNomContainingIgnoreCase(String nom);

    /** Recherche par principe actif */
    List<MedicamentEntity> findByPrincipeActifContainingIgnoreCase(String principeActif);

    /** Recherche combinée nom OU principe actif */
    @Query("SELECT m FROM MedicamentEntity m WHERE " +
            "LOWER(m.nom) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(m.principeActif) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<MedicamentEntity> searchByNomOrPrincipeActif(@Param("query") String query);

    /** Vérifie si un médicament existe déjà par nom exact */
    Optional<MedicamentEntity> findByNomIgnoreCase(String nom);

    /** Vérifie si un médicament existe déjà par principe actif exact */
    Optional<MedicamentEntity> findByPrincipeActifIgnoreCase(String principeActif);

    boolean existsByNomIgnoreCase(String nom);
}