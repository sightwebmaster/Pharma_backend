package com.pharmaApp.user.infrastructure.adapter.output.keycloak.dto;

import java.util.List;
import java.util.Map;

public class KeycloakUserRequest {

    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled = true;
    private boolean emailVerified = true;
    private List<KeycloakCredential> credentials;

    // Attributs custom (ex: role métier stocké en attribut Keycloak)
    private Map<String, List<String>> attributes;

    public KeycloakUserRequest(String email, String motDePasse,
                               String nom, String prenom, String role) {
        this.username      = email;
        this.email         = email;
        this.firstName     = prenom;
        this.lastName      = nom;
        this.enabled       = true;
        this.emailVerified = true;
        this.credentials   = List.of(new KeycloakCredential(motDePasse));
        // On stocke le rôle métier comme attribut custom Keycloak
        this.attributes    = Map.of("role", List.of(role));
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public boolean isEnabled() { return enabled; }
    public boolean isEmailVerified() { return emailVerified; }
    public List<KeycloakCredential> getCredentials() { return credentials; }
    public Map<String, List<String>> getAttributes() { return attributes; }
}