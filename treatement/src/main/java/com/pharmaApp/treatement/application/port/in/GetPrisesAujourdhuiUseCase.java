package com.pharmaApp.treatement.application.port.in;

import com.pharmaApp.treatement.application.dto.PriseResponse;
import java.util.List;

public interface GetPrisesAujourdhuiUseCase {
    List<PriseResponse> getPrisesAujourdhui(String patientUserId);
}