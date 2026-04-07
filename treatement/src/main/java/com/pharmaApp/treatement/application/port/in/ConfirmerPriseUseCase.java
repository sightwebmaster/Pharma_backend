package com.pharmaApp.treatement.application.port.in;


import com.pharmaApp.treatement.application.dto.ConfirmerPriseCommand;

public interface ConfirmerPriseUseCase {
    void confirmer(ConfirmerPriseCommand command);
}