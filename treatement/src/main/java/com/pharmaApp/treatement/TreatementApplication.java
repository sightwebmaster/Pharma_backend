package com.pharmaApp.treatement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PharmaCare — treatment-service
 * Point d'entrée Spring Boot
 *
 * Port : 8084 (configuré dans application.yml)
 * Architecture : Hexagonale (Ports & Adapters)
 * Base de données : MySQL — treatment_db
 */
@SpringBootApplication
public class TreatementApplication {

	public static void main(String[] args) {
		SpringApplication.run(TreatementApplication.class, args);
	}
}