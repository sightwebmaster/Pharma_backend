// UpdatePatientProfileRequest.java
package com.pharmaApp.user.application.dto.request;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class UpdatePatientProfileRequest {
    private String       nom;
    private String       prenom;
    private String       telephone;
    private LocalDate    dateNaissance;
    private String       groupeSanguin;
    private List<String> allergies;
    private List<String> maladiesChroniques;
}
