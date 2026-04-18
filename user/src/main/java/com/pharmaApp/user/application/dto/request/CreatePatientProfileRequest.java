// CreatePatientProfileRequest.java
package com.pharmaApp.user.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreatePatientProfileRequest {
    @NotBlank  private String       nom;
    @NotBlank  private String       prenom;
    @NotBlank @Email     private String       email;
    private String       telephone;
    @NotNull   private LocalDate    dateNaissance;
    private String       groupeSanguin;
    private Boolean      enceinte;
    private List<String> allergies;
    private List<String> maladiesChroniques;
    private String qrCode;
}
