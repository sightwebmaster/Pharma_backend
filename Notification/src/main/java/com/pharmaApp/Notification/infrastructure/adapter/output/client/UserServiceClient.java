package com.pharmaApp.Notification.infrastructure.adapter.output.client;

import com.pharmaApp.Notification.application.dto.ProcheResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class UserServiceClient {

    private static final ParameterizedTypeReference<List<ProcheResponse>> PROCHE_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String internalApiToken;

    public UserServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${pharmaApp.services.user-service}") String userServiceBaseUrl,
            @Value("${pharmaApp.internal-token}") String internalApiToken) {
        this.restClient = restClientBuilder.baseUrl(userServiceBaseUrl).build();
        this.internalApiToken = internalApiToken;
    }

    public List<ProcheResponse> getFollowers(String patientUserId) {
        try {
            List<ProcheResponse> response = restClient.get()
                    .uri("/api/v1/patients/{userId}/followers", patientUserId)
                    .header("X-Internal-Token", internalApiToken)
                    .retrieve()
                    .body(PROCHE_LIST_TYPE);
            return response != null ? response : List.of();
        } catch (RestClientException e) {
            log.error("Impossible de recuperer les followers du patient {} : {}",
                    patientUserId, e.getMessage());
            return List.of();
        }
    }
}
