package com.pharmaApp.medication.application.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MedicamentResponse {
    private String       id;
    private String       nom;
    private String       principeActif;
    private String       dosage;
    private String       forme;
    private String       description;
    private List<String> symptomes;
    private List<String> contreIndications;
    private List<String> effetsSecondaires;
    private List<String> allergenes;
    private List<String> restrictionsAge;
    private String       prix;
    private String       source;
}
