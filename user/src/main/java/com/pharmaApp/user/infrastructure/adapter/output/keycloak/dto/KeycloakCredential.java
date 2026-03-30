package com.pharmaApp.user.infrastructure.adapter.output.keycloak.dto;

public class KeycloakCredential {

    private String type  = "password";
    private String value;
    private boolean temporary = false;  // false = pas obligé de changer au 1er login

    public KeycloakCredential(String value) {
        this.value = value;
    }

    public String getType() { return type; }
    public String getValue() { return value; }
    public boolean isTemporary() { return temporary; }
}