package com.pharmaApp.treatement.application.port.in;


import com.pharmaApp.treatement.application.dto.ConfirmerPriseCommand;
import com.pharmaApp.treatement.application.dto.PriseResponse;

public interface ConfirmerPriseUseCase {
    PriseResponse confirmer(ConfirmerPriseCommand command);
}