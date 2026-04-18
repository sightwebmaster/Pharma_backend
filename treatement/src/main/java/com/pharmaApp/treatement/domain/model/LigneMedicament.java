package com.pharmaApp.treatement.domain.model;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.pharmaApp.treatement.application.dto.PlanifierTraitementCommand;

/**
 * Value Object — LigneMedicament (version corrigée)
 *
 * Getters requis par Traitement.java et TraitementJpaAdapter :
 *   getMedicamentId(), getMedicamentNom(), getPrincipeActif(),
 *   getDosage(), getDureeJours(), getHeuresPrise(), getInstructions()
 */
public class LigneMedicament {

    private final String          id;
    private final String          medicamentId;
    private final String          medicamentNom;
    private final String          principeActif;
    private final String          dosage;
    private final int             dureeJours;
    private final List<LocalTime> heuresPrise;
    private final String          instructions;
    public LigneMedicament(
            String id,
            String          medicamentId,
            String          medicamentNom,
            String          principeActif,
            String          dosage,
            int             dureeJours,
            List<LocalTime> heuresPrise,
            String          instructions
            ) {

        //   Objects.requireNonNull(medicamentId,  "medicamentId obligatoire");
        Objects.requireNonNull(medicamentNom, "medicamentNom obligatoire");
        Objects.requireNonNull(principeActif, "principeActif obligatoire");
        Objects.requireNonNull(dosage,        "dosage obligatoire");
        Objects.requireNonNull(heuresPrise,   "heuresPrise obligatoire");

        if (dureeJours <= 0) {
            throw new IllegalArgumentException("La durée doit être > 0 jour");
        }
        if (heuresPrise.isEmpty()) {
            throw new IllegalArgumentException(
                    "Au moins une heure de prise requise pour " + medicamentNom);
        }
        this.id = id;
        this.medicamentId  = medicamentId;
        this.medicamentNom = medicamentNom;
        this.principeActif = principeActif.toLowerCase().trim();
        this.dosage        = dosage;
        this.dureeJours    = dureeJours;
        this.heuresPrise   = Collections.unmodifiableList(heuresPrise);
        this.instructions  = instructions;
    }

    // =================================================================
    // GETTERS COMPLETS
    // =================================================================
    public String          getMedicamentId()  { return medicamentId; }
    public String          getMedicamentNom() { return medicamentNom; }
    public String          getPrincipeActif() { return principeActif; }
    public String          getDosage()        { return dosage; }
    public int             getDureeJours()    { return dureeJours; }
    public List<LocalTime> getHeuresPrise()   { return heuresPrise; }
    public String          getInstructions()  { return instructions; }
    public String          getId()             {return  id;}

    public int nombreTotalPrises() {
        return dureeJours * heuresPrise.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LigneMedicament l)) return false;
        return principeActif.equals(l.principeActif);
    }

    @Override
    public int hashCode() {
        return Objects.hash(principeActif);
    }
}