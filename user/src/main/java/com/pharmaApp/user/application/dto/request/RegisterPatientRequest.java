package com.pharmaApp.user.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class RegisterPatientRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Mot de passe minimum 8 caractères")
    private String motDePasse;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Téléphone invalide")
    private String telephone;

    private LocalDate dateNaissance;
    private String groupeSanguin;
    private Boolean enceinte;
    private List<String> allergies;
    private List<String> maladiesChroniques;
}
