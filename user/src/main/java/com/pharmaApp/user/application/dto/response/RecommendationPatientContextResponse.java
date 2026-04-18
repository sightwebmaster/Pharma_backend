package com.pharmaApp.user.application.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class RecommendationPatientContextResponse {
    private String userId;
    private Integer age;
    private Boolean pregnant;
    private List<String> allergies;
    private List<String> conditions;
    private List<String> maladiesChroniques;
    private String groupeSanguin;
}
