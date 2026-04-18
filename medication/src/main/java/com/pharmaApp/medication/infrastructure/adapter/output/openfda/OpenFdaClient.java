// ─────────────────────────────────────────────────────────────
// FICHIER 1 : OpenFdaClient.java
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.medication.infrastructure.adapter.output.openfda;

import com.pharmaApp.medication.infrastructure.adapter.output.persistence.entity.MedicamentEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Client OpenFDA — recherche de médicaments via l'API FDA.
 * https://api.fda.gov/drug/label.json?search=openfda.brand_name:paracetamol
 */
@Slf4j
@Component
public class OpenFdaClient {

    @Value("${pharmaApp.openfda.base-url}")
    private String baseUrl;

    @Value("${pharmaApp.openfda.api-key:}")
    private String apiKey;

    @Value("${pharmaApp.openfda.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<MedicamentEntity> rechercher(String query) {
        if (!enabled) {
            log.debug("OpenFDA désactivé — skip");
            return List.of();
        }

        try {
            String apiKeyParam = apiKey != null && !apiKey.isBlank()
                    ? "&api_key=" + apiKey : "";

            String url = baseUrl + "/label.json?search=openfda.brand_name:"
                    + query + apiKeyParam + "&limit=5";

            log.debug("Appel OpenFDA: {}", url);

            @SuppressWarnings("unchecked")
            Map<String, Object> response =
                    restTemplate.getForObject(url, Map.class);

            if (response == null) return List.of();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.get("results");

            if (results == null || results.isEmpty()) return List.of();

            List<MedicamentEntity> medicaments = new ArrayList<>();
            for (Map<String, Object> result : results) {
                MedicamentEntity med = parseOpenFdaResult(result);
                if (med != null) medicaments.add(med);
            }

            return medicaments;

        } catch (Exception e) {
            log.warn("OpenFDA indisponible pour '{}': {}", query, e.getMessage());
            return List.of(); // Fail-open — ne bloque pas l'application
        }
    }

    @SuppressWarnings("unchecked")
    private MedicamentEntity parseOpenFdaResult(Map<String, Object> result) {
        try {
            Map<String, Object> openFda =
                    (Map<String, Object>) result.get("openfda");
            if (openFda == null) return null;

            List<String> brandNames =
                    (List<String>) openFda.get("brand_name");
            List<String> genericNames =
                    (List<String>) openFda.get("generic_name");

            String nom = brandNames != null && !brandNames.isEmpty()
                    ? brandNames.get(0) : "Inconnu";
            String principeActif = genericNames != null && !genericNames.isEmpty()
                    ? genericNames.get(0) : nom;

            List<String> warnings = extractList(result, "warnings");
            List<String> contraindications = extractList(result, "contraindications");
            List<String> adverse = extractList(result, "adverse_reactions");

            return MedicamentEntity.builder()
                    .nom(nom)
                    .principeActif(principeActif)
                    .contreIndications(contraindications)
                    .effetsSecondaires(adverse)
                    .allergenes(List.of())
                    .symptomes(List.of())
                    .restrictionsAge(List.of())
                    .source("openFDA")
                    .build();

        } catch (Exception e) {
            log.warn("Erreur parsing OpenFDA result: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractList(Map<String, Object> result, String key) {
        try {
            Object val = result.get(key);
            if (val instanceof List) {
                List<String> list = (List<String>) val;
                return list.isEmpty() ? List.of()
                        : List.of(list.get(0).substring(0,
                        Math.min(500, list.get(0).length())));
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }
}