package com.pharmaApp.treatement.application.port.in;

import com.pharmaApp.treatement.application.dto.ModifierTraitementCommand;
import com.pharmaApp.treatement.application.dto.TraitementResponse;

public interface ModifierTraitementUseCase {
    TraitementResponse modifier(ModifierTraitementCommand command);
}
