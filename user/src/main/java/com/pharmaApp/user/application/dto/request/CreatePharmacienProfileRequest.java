
// CreatePharmacienProfileRequest.java
package com.pharmaApp.user.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreatePharmacienProfileRequest {
    @NotBlank private String nom;
    @NotBlank private String prenom;
    @Email    private String email;
    private String telephone;
    @NotBlank private String numeroOrdre;
    private String specialite;
}
