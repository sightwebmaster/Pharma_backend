package com.pharmaApp.medication.application.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ContreIndicationResponse {
    private boolean      safe;           // true = aucune contre-indication
    private String       medicamentNom;
    private List<String> alertes;        // liste des problèmes détectés
    private List<String> avertissements; // problèmes mineurs / à surveiller
}