
package com.pharmaApp.treatement.application.port.in;

import com.pharmaApp.treatement.application.dto.PlanifierTraitementCommand;
import com.pharmaApp.treatement.application.dto.TraitementResponse;

public interface PlanifierTraitementUseCase {
    TraitementResponse planifier(PlanifierTraitementCommand command);
}