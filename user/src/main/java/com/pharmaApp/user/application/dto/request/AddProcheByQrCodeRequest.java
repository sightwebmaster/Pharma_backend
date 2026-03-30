package com.pharmaApp.user.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddProcheByQrCodeRequest {

    @NotBlank(message = "userId obligatoire")
    private String procheUserId;

    @NotBlank(message = "Relation obligatoire")
    private String relation;
}