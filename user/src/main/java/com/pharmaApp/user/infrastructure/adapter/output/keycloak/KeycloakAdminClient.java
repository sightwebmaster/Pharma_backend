package com.pharmaApp.user.infrastructure.adapter.output.keycloak;

import com.pharmaApp.user.domain.port.output.KeycloakUserCreationPort;
import com.pharmaApp.user.infrastructure.adapter.output.keycloak.dto.KeycloakUserRequest;
import com.pharmaApp.user.infrastructure.config.KeycloakAdminConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAdminClient implements KeycloakUserCreationPort {

    private final KeycloakAdminConfig config;
    private final RestTemplate restTemplate; // ✅ injecté par Spring


    // ================= TOKEN =================
    public String getAdminAccessToken() {
        String url = config.getServerUrl()
                + "/realms/" + config.getRealm()
                + "/protocol/openid-connect/token";

        log.info("🔑 URL: {}", url);
        log.info("🔑 client_id: {}", config.getClientId());
        log.info("🔑 client_secret: {}", config.getClientSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // ✅ String directement — pas de MultiValueMap
        String body = "grant_type=client_credentials"
                + "&client_id=" + config.getClientId()
                + "&client_secret=" + config.getClientSecret();

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (response.getBody() == null || response.getBody().get("access_token") == null) {
                throw new RuntimeException("Token vide");
            }

            String token = (String) response.getBody().get("access_token");
            log.info("✅ Token obtenu");
            return token;

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("❌ Status: {}", e.getStatusCode());
            log.error("❌ Body: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Token admin impossible", e);
        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            throw new RuntimeException("Token admin impossible", e);
        }
    }

    // ================= CREATE USER =================
    @Override
    public String createUser(String email, String password,
                             String nom, String prenom, String role) {

        String token = getAdminAccessToken();

        String url = config.getServerUrl()
                + "/admin/realms/" + config.getRealm() + "/users";

        HttpHeaders headers = buildHeaders(token);

        KeycloakUserRequest userRequest =
                new KeycloakUserRequest(email, password, nom, prenom, role);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(userRequest, headers),
                    Void.class
            );

            String location = response.getHeaders().getLocation().toString();
            String userId = location.substring(location.lastIndexOf("/") + 1);

            log.info("✅ User créé: {}", userId);

            addRoleToUser(userId, role, token);

            return userId;

        } catch (HttpClientErrorException e) {
            log.error("❌ CreateUser error: {}", e.getResponseBodyAsString());
            throw new RuntimeException(e.getResponseBodyAsString(), e);
        }
    }

    // ================= ADD ROLE =================
    private void addRoleToUser(String userId, String roleName, String token) {

        HttpHeaders headers = buildHeaders(token);

        try {
            String rolesUrl = config.getServerUrl()
                    + "/admin/realms/" + config.getRealm() + "/roles";

            ResponseEntity<List> rolesResponse = restTemplate.exchange(
                    rolesUrl, HttpMethod.GET,
                    new HttpEntity<>(headers),
                    List.class
            );

            List<Map<String, Object>> roles = rolesResponse.getBody();

            Map<String, Object> role = roles.stream()
                    .filter(r -> roleName.equals(r.get("name")))
                    .findFirst()
                    .orElseGet(() -> createRole(roleName, token));

            String assignUrl = config.getServerUrl()
                    + "/admin/realms/" + config.getRealm()
                    + "/users/" + userId + "/role-mappings/realm";

            restTemplate.exchange(
                    assignUrl, HttpMethod.POST,
                    new HttpEntity<>(List.of(role), headers),
                    Void.class
            );

            log.info("✅ Rôle {} assigné", roleName);

        } catch (Exception e) {
            log.error("❌ Erreur assignation rôle", e);
        }
    }

    // ================= CREATE ROLE =================
    private Map<String, Object> createRole(String roleName, String token) {

        String url = config.getServerUrl()
                + "/admin/realms/" + config.getRealm() + "/roles";

        HttpHeaders headers = buildHeaders(token);

        Map<String, String> roleBody = Map.of("name", roleName);

        restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(roleBody, headers),
                Void.class // 🔥 FIX: Keycloak retourne 204
        );

        log.info("✅ Rôle créé: {}", roleName);

        return Map.of("name", roleName);
    }

    // ================= COMMON HEADERS =================
    private HttpHeaders buildHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}