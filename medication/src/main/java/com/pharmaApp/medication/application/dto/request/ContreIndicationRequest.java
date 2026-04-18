package com.pharmaApp.medication.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class ContreIndicationRequest {

    @NotBlank(message = "L'ID du médicament est obligatoire")
    private String medicamentId;

    /** Allergies connues du patient (ex: ["pénicilline", "aspirine"]) */
    private List<String> allergies;

    /** Maladies chroniques (ex: ["insuffisance rénale", "diabète"]) */
    private List<String> maladiesChroniques;

    /** Âge du patient (pour vérifier restrictions âge) */
    private Integer age;

    /** Grossesse */
    private boolean enceinte;

    /** Allaitement */
    private boolean allaitement;
}