// ─────────────────────────────────────────────────────────────
// FICHIER 5 : MedicamentDataInitializer.java
// Charge ~15 médicaments français au démarrage si DB vide
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.medication.infrastructure.config;

import com.pharmaApp.medication.infrastructure.adapter.output.persistence.entity.MedicamentEntity;
import com.pharmaApp.medication.infrastructure.adapter.output.persistence.repository.MedicamentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MedicamentDataInitializer implements ApplicationRunner {

    private final MedicamentJpaRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            log.info("DB médicaments déjà initialisée ({} entrées)", repository.count());
            return;
        }

        log.info("Initialisation des données médicaments...");
        repository.saveAll(getMedicamentsMock());
        log.info("✅ {} médicaments chargés", repository.count());
    }

    private List<MedicamentEntity> getMedicamentsMock() {
        return List.of(

                MedicamentEntity.builder()
                        .nom("Doliprane")
                        .principeActif("Paracétamol")
                        .dosage("500mg / 1000mg")
                        .forme("Comprimé")
                        .description("Antalgique et antipyrétique")
                        .symptomes(List.of("maux de tête", "fièvre", "douleur", "migraine"))
                        .contreIndications(List.of("insuffisance hépatique", "allergie paracétamol"))
                        .effetsSecondaires(List.of("réactions allergiques rares", "atteinte hépatique en surdosage"))
                        .allergenes(List.of("paracétamol"))
                        .restrictionsAge(List.of())
                        .prix("2.50€")
                        .source("interne")
                        .build(),

                MedicamentEntity.builder()
                        .nom("Amoxicilline")
                        .principeActif("Amoxicilline")
                        .dosage("500mg / 1g")
                        .forme("Gélule")
                        .description("Antibiotique de la famille des pénicillines")
                        .symptomes(List.of("infection bactérienne", "angine", "otite", "sinusite", "bronchite"))
                        .contreIndications(List.of("allergie pénicilline", "mononucléose infectieuse", "insuffisance rénale sévère"))
                        .effetsSecondaires(List.of("diarrhée", "nausées", "éruption cutanée", "candidose"))
                        .allergenes(List.of("pénicilline", "amoxicilline"))
                        .restrictionsAge(List.of())
                        .prix("4.50€")
                        .source("interne")
                        .build(),

                MedicamentEntity.builder()
                        .nom("Ibuprofène")
                        .principeActif("Ibuprofène")
                        .dosage("200mg / 400mg")
                        .forme("Comprimé")
                        .description("Anti-inflammatoire non stéroïdien (AINS)")
                        .symptomes(List.of("douleur", "inflammation", "fièvre", "maux de tête", "douleurs musculaires", "douleur thoracique"))
                        .contreIndications(List.of("ulcère gastroduodénal", "insuffisance rénale", "grossesse 3e trimestre", "allergie AINS"))
                        .effetsSecondaires(List.of("douleurs gastriques", "nausées", "vertiges", "risque cardiovasculaire"))
                        .allergenes(List.of("ibuprofène", "AINS"))
                        .restrictionsAge(List.of("enfant_moins_12", "grossesse"))
                        .prix("3.20€")
                        .source("interne")
                        .build(),

                MedicamentEntity.builder()
                        .nom("Aspirine")
                        .principeActif("Acide acétylsalicylique")
                        .dosage("500mg")
                        .forme("Comprimé effervescent")
                        .description("Antalgique, antipyrétique, anti-inflammatoire, anticoagulant")
                        .symptomes(List.of("maux de tête", "fièvre", "douleur", "douleur thoracique"))
                        .contreIndications(List.of("ulcère gastrique", "grossesse", "allergie aspirine", "hémophilie", "insuffisance rénale"))
                        .effetsSecondaires(List.of("saignements gastro-intestinaux", "acouphènes", "allergie"))
                        .allergenes(List.of("aspirine", "salicylés", "AINS"))
                        .restrictionsAge(List.of("enfant_moins_12", "grossesse", "allaitement"))
                        .prix("2.80€")
                        .source("interne")
                        .build(),

                MedicamentEntity.builder()
                        .nom("Ventoline")
                        .principeActif("Salbutamol")
                        .dosage("100µg/dose")
                        .forme("Aérosol")
                        .description("Bronchodilatateur — traitement de l'asthme")
                        .symptomes(List.of("asthme", "dyspnée", "essoufflement", "sifflement respiratoire"))
                        .contreIndications(List.of("allergie salbutamol", "tachycardie"))
                        .effetsSecondaires(List.of("tremblements", "tachycardie", "céphalées", "hypokaliémie"))
                        .allergenes(List.of("salbutamol"))
                        .restrictionsAge(List.of())
                        .prix("5.80€")
                        .source("interne")
                        .build(),

                MedicamentEntity.builder()
                        .nom("Metformine")
                        .principeActif("Metformine")
                        .dosage("500mg / 850mg / 1000mg")
                        .forme("Comprimé")
                        .description("Antidiabétique oral — traitement du diabète de type 2")
                        .symptomes(List.of("hyperglycémie", "diabète type 2"))
                        .contreIndications(List.of("insuffisance rénale", "insuffisance hépatique", "insuffisance cardiaque", "alcoolisme"))
                        .effetsSecondaires(List.of("nausées", "diarrhée", "douleurs abdominales", "acidose lactique rare"))
                        .allergenes(List.of("metformine"))
                        .restrictionsAge(List.of())
                        .prix("3.10€")
                        .source("interne")
                        .build(),

                MedicamentEntity.builder()
                        .nom("Oméprazole")
                        .principeActif("Oméprazole")
                        .dosage("20mg / 40mg")
                        .forme("Gélule")
                        .description("Inhibiteur de la pompe à protons — traitement de l'ulcère")
                        .symptomes(List.of("brûlures d'estomac", "reflux gastrique", "ulcère", "gastrite"))
                        .contreIndications(List.of("allergie oméprazole", "allergie inhibiteurs pompe à protons"))
                        .effetsSecondaires(List.of("céphalées", "diarrhée", "nausées", "flatulences"))
                        .allergenes(List.of("oméprazole"))
                        .restrictionsAge(List.of())
                        .prix("4.20€")
                        .source("interne")
                        .build(),

                MedicamentEntity.builder()
                        .nom("Lisinopril")
                        .principeActif("Lisinopril")
                        .dosage("5mg / 10mg / 20mg")
                        .forme("Comprimé")
                        .description("Inhibiteur de l'enzyme de conversion — traitement hypertension")
                        .symptomes(List.of("hypertension", "insuffisance cardiaque", "protection rénale diabète"))
                        .contreIndications(List.of("grossesse", "angioedème", "hyperkaliémie", "sténose artère rénale"))
                        .effetsSecondaires(List.of("toux sèche", "hypotension", "hyperkaliémie", "insuffisance rénale"))
                        .allergenes(List.of("IEC", "lisinopril"))
                        .restrictionsAge(List.of("grossesse"))
                        .prix("5.50€")
                        .source("interne")
                        .build(),

                MedicamentEntity.builder()
                        .nom("Atorvastatine")
                        .principeActif("Atorvastatine")
                        .dosage("10mg / 20mg / 40mg")
                        .forme("Comprimé")
                        .description("Statine — réduction du cholestérol")
                        .symptomes(List.of("hypercholestérolémie", "prévention cardiovasculaire"))
                        .contreIndications(List.of("grossesse", "allaitement", "myopathie", "insuffisance hépatique"))
                        .effetsSecondaires(List.of("douleurs musculaires", "rhabdomyolyse rare", "augmentation enzymes hépatiques"))
                        .allergenes(List.of("statines", "atorvastatine"))
                        .restrictionsAge(List.of("grossesse", "allaitement"))
                        .prix("6.80€")
                        .source("interne")
                        .build(),

                MedicamentEntity.builder()
                        .nom("Cetirizine")
                        .principeActif("Cétirizine")
                        .dosage("10mg")
                        .forme("Comprimé")
                        .description("Antihistaminique — traitement des allergies")
                        .symptomes(List.of("allergie", "rhinite allergique", "urticaire", "démangeaisons", "yeux qui piquent"))
                        .contreIndications(List.of("insuffisance rénale sévère", "allergie cétirizine"))
                        .effetsSecondaires(List.of("somnolence légère", "sécheresse buccale", "céphalées"))
                        .allergenes(List.of("cétirizine"))
                        .restrictionsAge(List.of())
                        .prix("3.50€")
                        .source("interne")
                        .build(),

                MedicamentEntity.builder()
                        .nom("Azithromycine")
                        .principeActif("Azithromycine")
                        .dosage("250mg / 500mg")
                        .forme("Comprimé")
                        .description("Antibiotique macrolide")
                        .symptomes(List.of("infection bactérienne", "pneumonie", "bronchite", "sinusite", "infections peau"))
                        .contreIndications(List.of("allergie macrolides", "allergie azithromycine", "problèmes cardiaques"))
                        .effetsSecondaires(List.of("nausées", "diarrhée", "douleurs abdominales", "troubles du rythme cardiaque"))
                        .allergenes(List.of("azithromycine", "macrolides"))
                        .restrictionsAge(List.of())
                        .prix("7.20€")
                        .source("interne")
                        .build(),

                MedicamentEntity.builder()
                        .nom("Prednisolone")
                        .principeActif("Prednisolone")
                        .dosage("5mg / 20mg")
                        .forme("Comprimé")
                        .description("Corticostéroïde — anti-inflammatoire puissant")
                        .symptomes(List.of("inflammation sévère", "asthme sévère", "allergie sévère", "maladies auto-immunes"))
                        .contreIndications(List.of("infection non traitée", "ulcère gastrique", "diabète non contrôlé", "ostéoporose"))
                        .effetsSecondaires(List.of("prise de poids", "hyperglycémie", "ostéoporose", "hypertension", "immunodépression"))
                        .allergenes(List.of("corticostéroïdes"))
                        .restrictionsAge(List.of("grossesse"))
                        .prix("4.90€")
                        .source("interne")
                        .build(),

                MedicamentEntity.builder()
                        .nom("Loratadine")
                        .principeActif("Loratadine")
                        .dosage("10mg")
                        .forme("Comprimé")
                        .description("Antihistaminique de 2e génération — non sédatif")
                        .symptomes(List.of("rhinite allergique", "urticaire", "allergie saisonnière", "démangeaisons"))
                        .contreIndications(List.of("allergie loratadine", "insuffisance hépatique sévère"))
                        .effetsSecondaires(List.of("céphalées", "somnolence rare", "sécheresse buccale"))
                        .allergenes(List.of("loratadine"))
                        .restrictionsAge(List.of())
                        .prix("3.80€")
                        .source("interne")
                        .build(),

                MedicamentEntity.builder()
                        .nom("Amlodipine")
                        .principeActif("Amlodipine")
                        .dosage("5mg / 10mg")
                        .forme("Comprimé")
                        .description("Inhibiteur calcique — traitement hypertension et angine")
                        .symptomes(List.of("hypertension", "angine de poitrine", "douleur thoracique"))
                        .contreIndications(List.of("choc cardiogénique", "allergie dihydropyridines", "sténose aortique sévère"))
                        .effetsSecondaires(List.of("oedèmes chevilles", "flush", "céphalées", "palpitations"))
                        .allergenes(List.of("amlodipine", "dihydropyridines"))
                        .restrictionsAge(List.of("grossesse"))
                        .prix("5.10€")
                        .source("interne")
                        .build(),

                MedicamentEntity.builder()
                        .nom("Levothyrox")
                        .principeActif("Lévothyroxine sodique")
                        .dosage("25µg / 50µg / 75µg / 100µg")
                        .forme("Comprimé")
                        .description("Hormone thyroïdienne de substitution")
                        .symptomes(List.of("hypothyroïdie", "fatigue chronique", "prise de poids inexpliquée"))
                        .contreIndications(List.of("hyperthyroïdie non traitée", "insuffisance coronarienne non traitée", "insuffisance surrénalienne"))
                        .effetsSecondaires(List.of("palpitations en surdosage", "insomnie", "perte de poids", "tremblements"))
                        .allergenes(List.of("lévothyroxine"))
                        .restrictionsAge(List.of())
                        .prix("3.90€")
                        .source("interne")
                        .build()
        );
    }
}