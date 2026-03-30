package com.pharmaApp.user.domain.port.output;

public interface KeycloakUserCreationPort {

    String createUser(String email,
                      String password,
                      String nom,
                      String prenom,
                      String role);
}