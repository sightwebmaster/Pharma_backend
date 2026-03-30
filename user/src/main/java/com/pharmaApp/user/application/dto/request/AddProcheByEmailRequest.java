package com.pharmaApp.user.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddProcheByEmailRequest {

    @NotBlank(message = "Email obligatoire")
    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Relation obligatoire")
    private String relation;
}