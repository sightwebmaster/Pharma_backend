package com.pharmaApp.user.domain.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proche {

    private String id;
    private String patientUserId;   // userId du patient connecté
    private String procheUserId;    // userId du patient lié
    private String relation;        // "Père", "Mère", "Frère"...
}