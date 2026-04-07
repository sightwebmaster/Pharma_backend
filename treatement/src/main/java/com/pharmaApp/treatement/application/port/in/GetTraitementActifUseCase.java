package com.pharmaApp.treatement.application.port.in;


import com.pharmaApp.treatement.application.dto.TraitementResponse;

public interface GetTraitementActifUseCase {
    TraitementResponse getTraitementActif(String patientUserId);
}