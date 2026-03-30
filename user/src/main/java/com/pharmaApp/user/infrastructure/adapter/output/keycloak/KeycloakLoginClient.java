    package com.pharmaApp.user.infrastructure.adapter.output.keycloak;

    import com.pharmaApp.user.application.dto.response.AuthResponse;
    import com.pharmaApp.user.infrastructure.config.KeycloakAdminConfig;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.http.*;
    import org.springframework.stereotype.Component;
    import org.springframework.util.LinkedMultiValueMap;
    import org.springframework.util.MultiValueMap;
    import org.springframework.web.client.RestTemplate;

    import java.util.Base64;
    import java.util.List;
    import java.util.Map;

    @Slf4j
    @Component
    @RequiredArgsConstructor
    public class KeycloakLoginClient {

        private final KeycloakAdminConfig config;
        private final RestTemplate restTemplate;

        public AuthResponse login(String email, String password) {
            String url = config.getServerUrl()
                    + "/realms/" + config.getRealm()
                    + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type",    "password");
            body.add("client_id",     config.getClientId());
            body.add("client_secret", config.getClientSecret());
            body.add("username",      email);
            body.add("password",      password);
            body.add("scope",         "openid profile email");

            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                        url, HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        Map.class
                );

                Map data = response.getBody();
                String accessToken  = (String) data.get("access_token");
                String refreshToken = (String) data.get("refresh_token");
                Long expiresIn      = Long.valueOf(data.get("expires_in").toString());
                String userId       = extractUserId(accessToken);
                String role         = extractRole(accessToken);

                return AuthResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .tokenType("Bearer")
                        .expiresIn(expiresIn)
                        .userId(userId)
                        .role(role)
                        .build();

            } catch (org.springframework.web.client.HttpClientErrorException e) {
                log.error("❌ Login error: {}", e.getResponseBodyAsString());
                throw new RuntimeException("Email ou mot de passe incorrect: c'est ça le prob");
            }
        }

        public String extractUserId(String token) {
            return (String) decodeToken(token).get("sub");
        }

        @SuppressWarnings("unchecked")
        private String extractRole(String token) {
            try {
                Map<String, Object> claims = decodeToken(token);
                Map<String, Object> realmAccess =
                        (Map<String, Object>) claims.get("realm_access");
                List<String> roles =
                        (List<String>) realmAccess.get("roles");
                return roles.stream()
                        .filter(r -> r.equals("PATIENT") || r.equals("PHARMACIEN"))
                        .findFirst()
                        .orElse("PATIENT");
            } catch (Exception e) {
                return "PATIENT";
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> decodeToken(String token) {
            try {
                String[] parts   = token.split("\\.");
                String payload   = new String(Base64.getUrlDecoder().decode(parts[1]));
                return new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(payload, Map.class);
            } catch (Exception e) {
                throw new RuntimeException("Token invalide");
            }
        }
    }