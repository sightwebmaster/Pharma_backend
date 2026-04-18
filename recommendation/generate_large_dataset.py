"""
Generate a large synthetic French medical dataset for PharmaCare.

Goals:
- >= 65 000 rows
- >= 220 unique diseases
- >= 5 200 unique medications
- French symptoms and advice
- Lightweight references only: template- and rule-based generation

Outputs:
- recommendation/data/medical_enriched_dataset_large.csv
- recommendation/data/medication_catalog_large.json

Important:
This generator builds a synthetic bootstrap dataset for prototyping and
training experiments. It is not a validated clinical knowledge base.
"""

from __future__ import annotations

import csv
import json
import random
import unicodedata
from dataclasses import dataclass
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parent
DATA_DIR = ROOT_DIR / "data"
DATASET_PATH = DATA_DIR / "medical_enriched_dataset_large.csv"
CATALOG_PATH = DATA_DIR / "medication_catalog_large.json"

ROWS_PER_DISEASE = 300
MEDICATIONS_PER_DISEASE = 24
SEED = 20260417


@dataclass(frozen=True)
class CategoryConfig:
    name: str
    source: str
    symptoms: list[str]
    secondary_symptoms: list[str]
    advice: list[str]
    contraindications: list[str]
    side_effects: list[str]
    dosage_units: list[str]
    forms: list[str]
    frequencies: list[str]
    med_roots: list[str]
    med_suffixes: list[str]
    diseases: list[str]


CATEGORIES: list[CategoryConfig] = [
    CategoryConfig(
        name="Cardiologie",
        source="synthetic_low_reference_cardio_v1",
        symptoms=[
            "douleur thoracique", "essoufflement", "palpitations", "fatigue",
            "gonflement des jambes", "vertiges", "oppression thoracique",
            "malaise à l'effort", "hypertension", "toux nocturne",
        ],
        secondary_symptoms=[
            "sueurs", "nausées", "orthopnée", "intolérance à l'effort",
            "tachycardie", "sensation de cœur irrégulier",
        ],
        advice=[
            "surveiller la tension artérielle", "réduire le sel",
            "consulter rapidement en cas d'aggravation",
            "éviter l'automédication cardiovasculaire",
            "maintenir une activité adaptée après avis médical",
        ],
        contraindications=[
            "grossesse sans avis spécialisé", "hypotension sévère",
            "insuffisance rénale avancée", "antécédent d'angioedème",
        ],
        side_effects=[
            "étourdissements", "toux sèche", "œdèmes", "fatigue", "bradycardie",
        ],
        dosage_units=["2,5 mg", "5 mg", "10 mg", "20 mg", "40 mg"],
        forms=["comprimé", "gélule", "solution buvable"],
        frequencies=["1 fois/jour", "2 fois/jour"],
        med_roots=["cardio", "vaso", "tenso", "rythmo", "arterio", "corono"],
        med_suffixes=["pril", "sartan", "olol", "dipine", "xaban", "statine"],
        diseases=[
            "Hypertension artérielle essentielle",
            "Insuffisance cardiaque chronique",
            "Angor stable",
            "Fibrillation auriculaire",
            "Cardiomyopathie dilatée",
            "Péricardite aiguë",
            "Myocardite",
            "Cardiopathie hypertensive",
            "Syndrome du QT long",
            "Hypertension pulmonaire",
        ],
    ),
    CategoryConfig(
        name="Pneumologie",
        source="synthetic_low_reference_pulm_v1",
        symptoms=[
            "toux", "essoufflement", "sifflements respiratoires", "fièvre",
            "douleur thoracique", "expectoration", "oppression thoracique",
            "dyspnée nocturne", "encombrement bronchique", "désaturation",
        ],
        secondary_symptoms=[
            "fatigue", "frissons", "gorge irritée", "cyanose", "respiration rapide",
        ],
        advice=[
            "hydrater correctement", "arrêter le tabac",
            "consulter en urgence si gêne respiratoire importante",
            "aérer le domicile", "respecter les traitements inhalés",
        ],
        contraindications=[
            "allergie connue au principe actif", "insuffisance respiratoire sévère non stabilisée",
            "grossesse à surveiller", "glaucome pour certains bronchodilatateurs",
        ],
        side_effects=[
            "tremblements", "tachycardie", "somnolence", "nausées", "bouche sèche",
        ],
        dosage_units=["100 µg", "200 µg", "400 µg", "250 mg", "500 mg"],
        forms=["aérosol doseur", "poudre inhalée", "sirop", "comprimé"],
        frequencies=["1 fois/jour", "2 fois/jour", "à la demande"],
        med_roots=["broncho", "pulmo", "respira", "alveo", "thoraco", "venti"],
        med_suffixes=["butamol", "sonide", "filine", "cort", "xol", "mycine"],
        diseases=[
            "Asthme persistant",
            "Bronchite aiguë",
            "BPCO",
            "Pneumonie communautaire",
            "Embolie pulmonaire",
            "Fibrose pulmonaire idiopathique",
            "Bronchectasies",
            "Pleurésie",
            "Mucoviscidose",
            "Syndrome d'apnées obstructives du sommeil",
        ],
    ),
    CategoryConfig(
        name="Gastro-entérologie",
        source="synthetic_low_reference_gastro_v1",
        symptoms=[
            "douleur abdominale", "nausées", "vomissements", "ballonnements",
            "diarrhée", "constipation", "brûlures d'estomac", "reflux", "perte d'appétit",
            "sensation de ventre gonflé",
        ],
        secondary_symptoms=[
            "éructations", "digestion lente", "crampes abdominales", "selles noires", "hoquet",
        ],
        advice=[
            "fractionner les repas", "éviter les aliments irritants",
            "boire régulièrement", "consulter en cas de sang dans les selles",
            "réduire l'alcool et les aliments gras",
        ],
        contraindications=[
            "insuffisance hépatique sévère", "occlusion digestive",
            "allergie digestive connue", "grossesse à évaluer selon le traitement",
        ],
        side_effects=[
            "diarrhée", "constipation", "nausées", "douleurs abdominales", "céphalées",
        ],
        dosage_units=["10 mg", "20 mg", "40 mg", "250 mg", "500 mg"],
        forms=["gélule", "suspension orale", "comprimé", "sachet"],
        frequencies=["1 fois/jour", "2 fois/jour", "avant les repas"],
        med_roots=["gastro", "hepato", "colo", "digesto", "ulcero", "pancreo"],
        med_suffixes=["prazole", "ride", "mine", "mycine", "setron", "lax"],
        diseases=[
            "Reflux gastro-œsophagien",
            "Ulcère gastro-duodénal",
            "Syndrome de l'intestin irritable",
            "Maladie de Crohn",
            "Rectocolite hémorragique",
            "Gastro-entérite aiguë",
            "Pancréatite chronique",
            "Lithiase biliaire",
            "Hépatite auto-immune",
            "Cirrhose compensée",
        ],
    ),
    CategoryConfig(
        name="Neurologie",
        source="synthetic_low_reference_neuro_v1",
        symptoms=[
            "céphalées", "vertiges", "troubles de la mémoire", "faiblesse musculaire",
            "engourdissements", "troubles de l'équilibre", "tremblements",
            "troubles de la parole", "photophobie", "raideur de nuque",
        ],
        secondary_symptoms=[
            "vision floue", "somnolence", "fourmillements", "crises", "hypersensibilité au bruit",
        ],
        advice=[
            "consulter rapidement si déficit neurologique brutal",
            "éviter la conduite si somnolence", "respecter les heures de sommeil",
            "tenir un carnet des symptômes", "réduire les facteurs déclenchants connus",
        ],
        contraindications=[
            "antécédent d'épilepsie non stabilisée", "grossesse selon la molécule",
            "insuffisance hépatique sévère", "glaucome à angle fermé",
        ],
        side_effects=[
            "somnolence", "étourdissements", "troubles digestifs", "bouche sèche", "fatigue",
        ],
        dosage_units=["5 mg", "25 mg", "50 mg", "100 mg", "250 mg"],
        forms=["comprimé", "gélule", "solution buvable"],
        frequencies=["1 fois/jour", "2 fois/jour", "au coucher"],
        med_roots=["neuro", "migra", "axono", "synapto", "cerebro", "vesti"],
        med_suffixes=["triptan", "zepam", "gabaline", "rigine", "done", "tan"],
        diseases=[
            "Migraine avec aura",
            "Épilepsie focale",
            "Maladie de Parkinson",
            "Sclérose en plaques",
            "Névralgie du trijumeau",
            "Neuropathie périphérique",
            "Méningite virale",
            "Accident ischémique transitoire",
            "Myasthénie",
            "Ataxie spinocérébelleuse",
        ],
    ),
    CategoryConfig(
        name="Endocrinologie",
        source="synthetic_low_reference_endo_v1",
        symptoms=[
            "fatigue", "soif importante", "perte de poids", "prise de poids",
            "polyurie", "intolérance au froid", "palpitations", "tremblements",
            "fringales", "troubles hormonaux",
        ],
        secondary_symptoms=[
            "sueurs", "constipation", "diarrhée", "sécheresse cutanée", "faiblesse musculaire",
        ],
        advice=[
            "faire un suivi biologique régulier", "éviter les sucres rapides excessifs",
            "surveiller la glycémie si nécessaire", "consulter en cas de malaise",
            "respecter les prises à heure fixe",
        ],
        contraindications=[
            "insuffisance rénale avancée", "grossesse sans adaptation",
            "déshydratation sévère", "allergie à la substance active",
        ],
        side_effects=[
            "hypoglycémie", "nausées", "vertiges", "palpitations", "diarrhée",
        ],
        dosage_units=["25 µg", "50 µg", "100 µg", "500 mg", "1000 mg"],
        forms=["comprimé", "stylo injectable", "solution injectable"],
        frequencies=["1 fois/jour", "2 fois/jour", "1 fois/semaine"],
        med_roots=["glyco", "thyro", "endo", "metabo", "insulo", "hormo"],
        med_suffixes=["gliflozine", "gliptine", "formine", "thyrox", "tide", "sterone"],
        diseases=[
            "Diabète de type 2",
            "Diabète de type 1",
            "Hypothyroïdie",
            "Hyperthyroïdie",
            "Syndrome des ovaires polykystiques",
            "Maladie d'Addison",
            "Syndrome de Cushing",
            "Acromégalie",
            "Hyperparathyroïdie primaire",
            "Phéochromocytome",
        ],
    ),
    CategoryConfig(
        name="Dermatologie",
        source="synthetic_low_reference_derm_v1",
        symptoms=[
            "éruption cutanée", "démangeaisons", "plaques rouges", "peau sèche",
            "lésions", "desquamation", "brûlure cutanée", "urticaire",
            "rougeur diffuse", "vésicules",
        ],
        secondary_symptoms=[
            "suintement", "croûtes", "fissures", "douleur locale", "sensibilité cutanée",
        ],
        advice=[
            "éviter les produits irritants", "hydrater la peau régulièrement",
            "ne pas gratter les lésions", "consulter si extension rapide",
            "protéger la peau du soleil",
        ],
        contraindications=[
            "allergie au produit topique", "infection cutanée non traitée",
            "grossesse selon la classe thérapeutique", "plaie profonde",
        ],
        side_effects=[
            "irritation locale", "sécheresse", "photosensibilité", "rougeur", "sensation de brûlure",
        ],
        dosage_units=["0,05 %", "0,1 %", "1 %", "10 mg", "20 mg"],
        forms=["crème", "pommade", "gel", "lotion", "comprimé"],
        frequencies=["1 fois/jour", "2 fois/jour", "application locale"],
        med_roots=["derma", "cuti", "epider", "pruri", "psoro", "myco"],
        med_suffixes=["cort", "azole", "mab", "cycline", "tine", "trine"],
        diseases=[
            "Eczéma atopique",
            "Psoriasis en plaques",
            "Acné inflammatoire",
            "Urticaire chronique",
            "Rosacée",
            "Dermatite séborrhéique",
            "Lichen plan",
            "Pemphigus vulgaire",
            "Sclérodermie cutanée",
            "Lupus cutané subaigu",
        ],
    ),
    CategoryConfig(
        name="Rhumatologie",
        source="synthetic_low_reference_rhumato_v1",
        symptoms=[
            "douleurs articulaires", "raideur matinale", "gonflement articulaire",
            "douleur lombaire", "fatigue", "raideur du dos", "douleur aux mains",
            "limitation des mouvements", "douleur inflammatoire", "boiterie",
        ],
        secondary_symptoms=[
            "rougeur articulaire", "chaleur locale", "douleur nocturne", "craquements", "spasmes",
        ],
        advice=[
            "maintenir une activité douce", "éviter le port de charges lourdes",
            "consulter en cas d'articulation rouge et chaude",
            "adapter les efforts", "surveiller l'évolution de la douleur",
        ],
        contraindications=[
            "ulcère gastro-duodénal", "grossesse pour certains traitements",
            "insuffisance rénale", "infection active",
        ],
        side_effects=[
            "douleurs gastriques", "nausées", "somnolence", "vertiges", "prise de poids",
        ],
        dosage_units=["50 mg", "100 mg", "200 mg", "400 mg", "15 mg"],
        forms=["comprimé", "gélule", "injection sous-cutanée", "gel"],
        frequencies=["1 fois/jour", "2 fois/jour", "1 fois/semaine"],
        med_roots=["arthro", "rhumo", "syno", "osteo", "spondo", "myal"],
        med_suffixes=["coxib", "fenac", "trexate", "mab", "sone", "profen"],
        diseases=[
            "Polyarthrite rhumatoïde",
            "Arthrose du genou",
            "Spondylarthrite ankylosante",
            "Goutte",
            "Fibromyalgie",
            "Lupus systémique",
            "Vascularite systémique",
            "Chondrocalcinose",
            "Maladie de Behçet",
            "Polymyosite",
        ],
    ),
    CategoryConfig(
        name="Néphrologie",
        source="synthetic_low_reference_nephro_v1",
        symptoms=[
            "œdèmes", "urines mousseuses", "douleur lombaire", "fatigue",
            "hypertension", "sang dans les urines", "diminution des urines",
            "brûlures urinaires", "nausées", "gonflement du visage",
        ],
        secondary_symptoms=[
            "fièvre", "frissons", "colique", "urines troubles", "prurit diffus",
        ],
        advice=[
            "surveiller la quantité d'urines", "boire selon avis médical",
            "consulter rapidement en cas d'anurie", "limiter l'automédication néphrotoxique",
            "contrôler la tension artérielle",
        ],
        contraindications=[
            "déshydratation sévère", "obstruction urinaire", "grossesse selon le traitement",
            "insuffisance hépatique avancée",
        ],
        side_effects=[
            "vertiges", "déshydratation", "nausées", "troubles ioniques", "hypotension",
        ],
        dosage_units=["5 mg", "10 mg", "25 mg", "40 mg", "500 mg"],
        forms=["comprimé", "gélule", "solution injectable"],
        frequencies=["1 fois/jour", "2 fois/jour"],
        med_roots=["nephro", "reno", "glomeru", "pyelo", "uro", "dia"],
        med_suffixes=["semide", "artan", "xime", "floxacine", "pril", "sterone"],
        diseases=[
            "Maladie rénale chronique",
            "Syndrome néphrotique",
            "Glomérulonéphrite aiguë",
            "Pyélonéphrite",
            "Colique néphrétique",
            "Polykystose rénale autosomique dominante",
            "Néphrite lupique",
            "Néphropathie diabétique",
            "Syndrome hémolytique et urémique",
            "Amylose rénale",
        ],
    ),
    CategoryConfig(
        name="Hématologie",
        source="synthetic_low_reference_hemato_v1",
        symptoms=[
            "fatigue", "pâleur", "essoufflement", "saignements faciles", "ecchymoses",
            "fièvre", "ganglions", "sueurs nocturnes", "perte de poids", "vertiges",
        ],
        secondary_symptoms=[
            "palpitations", "saignement gingival", "infections répétées", "douleur osseuse", "faiblesse",
        ],
        advice=[
            "faire un bilan sanguin complet", "consulter en cas de saignement important",
            "éviter l'automédication anti-inflammatoire sans avis", "surveiller les signes infectieux",
            "ne pas retarder l'évaluation spécialisée",
        ],
        contraindications=[
            "aplatissement médullaire sévère", "grossesse selon les traitements",
            "infection active non contrôlée", "allergie au produit sanguin",
        ],
        side_effects=[
            "nausées", "céphalées", "constipation", "fatigue", "réactions cutanées",
        ],
        dosage_units=["1 mg", "5 mg", "10 mg", "100 mg", "300 mg"],
        forms=["comprimé", "gélule", "solution injectable"],
        frequencies=["1 fois/jour", "2 fois/jour", "1 fois/semaine"],
        med_roots=["hemo", "erythro", "leuco", "coagu", "lympho", "myelo"],
        med_suffixes=["fer", "mab", "xaban", "filgrast", "plase", "tinib"],
        diseases=[
            "Anémie ferriprive",
            "Anémie hémolytique",
            "Purpura thrombopénique immunologique",
            "Hémophilie A",
            "Leucémie aiguë myéloïde",
            "Lymphome hodgkinien",
            "Myélome multiple",
            "Drépanocytose",
            "Syndrome myélodysplasique",
            "Thrombocytémie essentielle",
        ],
    ),
    CategoryConfig(
        name="Infectiologie",
        source="synthetic_low_reference_infectio_v1",
        symptoms=[
            "fièvre", "frissons", "courbatures", "fatigue", "toux",
            "maux de gorge", "diarrhée", "éruption", "adénopathies", "douleurs diffuses",
        ],
        secondary_symptoms=[
            "perte d'appétit", "sueurs", "nausées", "céphalées", "déshydratation",
        ],
        advice=[
            "respecter strictement la durée du traitement", "s'isoler si contagieux",
            "bien s'hydrater", "consulter si aggravation rapide",
            "éviter l'automédication antibiotique",
        ],
        contraindications=[
            "allergie aux antibiotiques concernés", "insuffisance hépatique sévère",
            "grossesse selon la molécule", "allongement du QT selon la molécule",
        ],
        side_effects=[
            "nausées", "diarrhée", "rash", "vertiges", "candidose",
        ],
        dosage_units=["250 mg", "500 mg", "750 mg", "1 g", "2 g"],
        forms=["comprimé", "gélule", "suspension orale", "solution injectable"],
        frequencies=["1 fois/jour", "2 fois/jour", "3 fois/jour"],
        med_roots=["infecto", "viralo", "bacterio", "myco", "septi", "parasi"],
        med_suffixes=["mycine", "cilline", "floxacine", "azole", "vir", "cycline"],
        diseases=[
            "Grippe saisonnière",
            "COVID-19 symptomatique",
            "Angine bactérienne",
            "Tuberculose pulmonaire",
            "Mononucléose infectieuse",
            "Brucellose",
            "Leishmaniose viscérale",
            "Fièvre typhoïde",
            "Paludisme non compliqué",
            "Dengue",
        ],
    ),
    CategoryConfig(
        name="Psychiatrie",
        source="synthetic_low_reference_psy_v1",
        symptoms=[
            "anxiété", "insomnie", "tristesse", "perte d'intérêt", "agitation",
            "fatigue psychique", "ruminations", "crises de panique", "irritabilité",
            "difficultés de concentration",
        ],
        secondary_symptoms=[
            "idées noires", "palpitations", "isolement", "hypersomnie", "anorexie",
        ],
        advice=[
            "évaluer le risque suicidaire sans délai si nécessaire",
            "éviter l'alcool et les sédatifs non prescrits", "associer un suivi psychologique",
            "respecter les horaires de coucher", "consulter rapidement en cas d'aggravation",
        ],
        contraindications=[
            "association à d'autres sédatifs sans avis", "grossesse selon la molécule",
            "glaucome pour certaines classes", "allergie connue à la substance active",
        ],
        side_effects=[
            "somnolence", "nausées", "prise de poids", "bouche sèche", "vertiges",
        ],
        dosage_units=["5 mg", "10 mg", "20 mg", "50 mg", "100 mg"],
        forms=["comprimé", "gélule", "solution buvable"],
        frequencies=["1 fois/jour", "au coucher", "2 fois/jour"],
        med_roots=["psycho", "calmo", "sereno", "thymo", "anxio", "somno"],
        med_suffixes=["xétine", "pram", "zepam", "pine", "done", "lamine"],
        diseases=[
            "Trouble anxieux généralisé",
            "Épisode dépressif majeur",
            "Trouble panique",
            "Trouble bipolaire",
            "Schizophrénie",
            "Insomnie chronique",
            "Trouble obsessionnel compulsif",
            "État de stress post-traumatique",
            "Anorexie mentale",
            "Trouble schizo-affectif",
        ],
    ),
    CategoryConfig(
        name="Ophtalmologie",
        source="synthetic_low_reference_ophtalmo_v1",
        symptoms=[
            "vision floue", "douleur oculaire", "rougeur oculaire", "larmoiement",
            "photophobie", "baisse de vision", "corps flottants", "sécheresse oculaire",
            "halo lumineux", "démangeaisons oculaires",
        ],
        secondary_symptoms=[
            "maux de tête", "sensation de sable", "écoulement", "œil collé", "vision double",
        ],
        advice=[
            "consulter en urgence si baisse brutale de vision", "éviter de se frotter les yeux",
            "retirer les lentilles si besoin", "protéger les yeux de la lumière forte",
            "respecter l'hygiène oculaire",
        ],
        contraindications=[
            "glaucome pour certains collyres", "allergie au conservateur",
            "lentilles de contact en phase aiguë", "infection non traitée",
        ],
        side_effects=[
            "picotements", "vision brouillée transitoire", "rougeur", "larmoiement", "irritation",
        ],
        dosage_units=["0,1 %", "0,5 %", "1 %", "5 mg"],
        forms=["collyre", "gel ophtalmique", "comprimé"],
        frequencies=["1 fois/jour", "2 fois/jour", "3 fois/jour"],
        med_roots=["ophta", "retino", "glauco", "lacri", "corneo", "uveo"],
        med_suffixes=["lol", "prost", "mide", "fen", "drine", "zolamide"],
        diseases=[
            "Conjonctivite allergique",
            "Glaucome chronique à angle ouvert",
            "Sécheresse oculaire sévère",
            "Uvée antérieure",
            "Dégénérescence maculaire liée à l'âge",
            "Kératite",
            "Blépharite chronique",
            "Décollement de rétine",
            "Névrite optique",
            "Rétinopathie diabétique",
        ],
    ),
    CategoryConfig(
        name="ORL",
        source="synthetic_low_reference_orl_v1",
        symptoms=[
            "maux de gorge", "douleur à l'oreille", "nez bouché", "écoulement nasal",
            "fièvre", "toux", "enrouement", "pression sinusienne", "baisse d'audition",
            "vertiges",
        ],
        secondary_symptoms=[
            "acouphènes", "sensation d'oreille bouchée", "mauvaise haleine", "éternuements", "frissons",
        ],
        advice=[
            "laver le nez si nécessaire", "consulter si douleurs auriculaires importantes",
            "boire chaud en cas de gorge irritée", "éviter la fumée",
            "consulter en urgence si détresse respiratoire",
        ],
        contraindications=[
            "allergie aux antibiotiques", "hypertension pour certains décongestionnants",
            "grossesse selon le traitement", "glaucome selon certains produits",
        ],
        side_effects=[
            "somnolence", "sécheresse buccale", "nausées", "vertiges", "palpitations",
        ],
        dosage_units=["5 mg", "10 mg", "250 mg", "500 mg", "1 g"],
        forms=["spray nasal", "sirop", "comprimé", "gouttes auriculaires"],
        frequencies=["1 fois/jour", "2 fois/jour", "3 fois/jour"],
        med_roots=["orlo", "sinuso", "pharyngo", "otico", "laryngo", "naso"],
        med_suffixes=["mycine", "xime", "adine", "zoline", "caïne", "fen"],
        diseases=[
            "Sinusite aiguë",
            "Otite moyenne aiguë",
            "Pharyngite",
            "Rhinite allergique",
            "Laryngite",
            "Maladie de Ménière",
            "Labyrinthite",
            "Polypose naso-sinusienne",
            "Surdité brusque",
            "Paralysie faciale périphérique",
        ],
    ),
    CategoryConfig(
        name="Gynécologie et obstétrique",
        source="synthetic_low_reference_gyneco_v1",
        symptoms=[
            "douleur pelvienne", "retard de règles", "saignements anormaux",
            "prurit intime", "écoulement vaginal", "nausées", "fatigue",
            "tension mammaire", "fièvre", "douleur pendant les rapports",
        ],
        secondary_symptoms=[
            "brûlures urinaires", "crampes", "ballonnements", "étourdissements", "irrégularité menstruelle",
        ],
        advice=[
            "consulter rapidement en cas de saignement abondant", "éviter l'automédication pendant la grossesse",
            "respecter l'hygiène intime douce", "effectuer un suivi gynécologique régulier",
            "consulter en urgence si douleur pelvienne aiguë intense",
        ],
        contraindications=[
            "grossesse selon la molécule", "allaitement selon la molécule",
            "thrombophilie", "insuffisance hépatique",
        ],
        side_effects=[
            "nausées", "céphalées", "sensibilité mammaire", "somnolence", "douleurs abdominales",
        ],
        dosage_units=["100 mg", "200 mg", "400 mg", "1 g", "5 mg"],
        forms=["ovule", "comprimé", "gel", "crème", "solution injectable"],
        frequencies=["1 fois/jour", "2 fois/jour", "application locale"],
        med_roots=["gyne", "utero", "ovario", "menso", "gravido", "pelvio"],
        med_suffixes=["gest", "azole", "mycine", "profen", "pristone", "terone"],
        diseases=[
            "Endométriose",
            "Vaginose bactérienne",
            "Candidose vulvo-vaginale",
            "Syndrome prémenstruel sévère",
            "Ménométrorragies fonctionnelles",
            "Prééclampsie",
            "Hyperémèse gravidique",
            "Grossesse extra-utérine",
            "Kyste ovarien compliqué",
            "Placenta prævia",
        ],
    ),
    CategoryConfig(
        name="Pédiatrie",
        source="synthetic_low_reference_pediatrie_v1",
        symptoms=[
            "fièvre", "toux", "refus de s'alimenter", "pleurs inhabituels",
            "vomissements", "diarrhée", "éruption", "somnolence", "respiration rapide",
            "agitation",
        ],
        secondary_symptoms=[
            "irritabilité", "déshydratation", "nez bouché", "convulsion fébrile", "sifflement",
        ],
        advice=[
            "surveiller l'hydratation de l'enfant", "consulter rapidement si respiration difficile",
            "ne pas donner de médicaments sans dose adaptée à l'âge", "contrôler la température",
            "revenir en urgence si léthargie ou convulsions",
        ],
        contraindications=[
            "posologie inadaptée au poids", "déshydratation sévère",
            "allergie au principe actif", "atteinte hépatique sévère",
        ],
        side_effects=[
            "somnolence", "nausées", "éruption", "diarrhée", "irritabilité",
        ],
        dosage_units=["60 mg", "120 mg", "250 mg", "5 ml", "10 ml"],
        forms=["sirop", "suspension orale", "suppositoire", "spray nasal"],
        frequencies=["1 fois/jour", "2 fois/jour", "3 fois/jour"],
        med_roots=["pedia", "infanto", "neo", "juve", "lacto", "croissa"],
        med_suffixes=["mol", "cilline", "mycine", "sone", "butamol", "dine"],
        diseases=[
            "Bronchiolite",
            "Laryngotrachéite",
            "Varicelle",
            "Rougeole",
            "Oreillons",
            "Coqueluche",
            "Otite séreuse de l'enfant",
            "Reflux gastro-œsophagien du nourrisson",
            "Maladie de Kawasaki",
            "Purpura rhumatoïde",
        ],
    ),
    CategoryConfig(
        name="Oncologie",
        source="synthetic_low_reference_onco_v1",
        symptoms=[
            "perte de poids", "fatigue", "douleur persistante", "fièvre",
            "ganglions", "toux chronique", "saignements", "masses palpables",
            "anémie", "sueurs nocturnes",
        ],
        secondary_symptoms=[
            "nausées", "vomissements", "dyspnée", "douleur osseuse", "anorexie",
        ],
        advice=[
            "ne pas retarder l'évaluation spécialisée", "surveiller la douleur et la nutrition",
            "consulter en cas de fièvre sous traitement", "maintenir un suivi rapproché",
            "éviter l'automédication sans l'équipe référente",
        ],
        contraindications=[
            "neutropénie sévère", "grossesse selon la molécule", "infection active",
            "atteinte hépatique majeure",
        ],
        side_effects=[
            "nausées", "fatigue", "mucite", "neutropénie", "rash",
        ],
        dosage_units=["50 mg", "100 mg", "200 mg", "400 mg", "600 mg"],
        forms=["comprimé", "gélule", "solution injectable"],
        frequencies=["1 fois/jour", "2 fois/jour", "1 fois/semaine"],
        med_roots=["onco", "tumoro", "cyto", "neo", "immuno", "radio"],
        med_suffixes=["tinib", "mab", "platin", "rubicine", "taxel", "trant"],
        diseases=[
            "Cancer du sein",
            "Cancer colorectal",
            "Cancer bronchique non à petites cellules",
            "Leucémie lymphoïde chronique",
            "Lymphome non hodgkinien",
            "Cancer de la prostate",
            "Cancer de l'ovaire",
            "Mélanome métastatique",
            "Sarcome d'Ewing",
            "Glioblastome",
        ],
    ),
    CategoryConfig(
        name="Immunologie et maladies auto-immunes",
        source="synthetic_low_reference_immuno_v1",
        symptoms=[
            "fatigue", "douleurs articulaires", "éruption", "fièvre",
            "aphtes", "sécheresse oculaire", "sécheresse buccale", "gonflement des articulations",
            "faiblesse musculaire", "douleurs diffuses",
        ],
        secondary_symptoms=[
            "raynaud", "perte de poids", "adénopathies", "prurit", "dyspnée",
        ],
        advice=[
            "effectuer un suivi spécialisé régulier", "éviter l'arrêt brutal des traitements",
            "signaler toute infection", "protéger la peau et les muqueuses",
            "surveiller la fatigue chronique",
        ],
        contraindications=[
            "infection active", "grossesse selon l'immunosuppresseur",
            "insuffisance hépatique sévère", "vaccin vivant selon le traitement",
        ],
        side_effects=[
            "nausées", "fatigue", "risque infectieux", "rash", "cytopénie",
        ],
        dosage_units=["5 mg", "10 mg", "20 mg", "50 mg", "100 mg"],
        forms=["comprimé", "gélule", "injection sous-cutanée", "perfusion"],
        frequencies=["1 fois/jour", "2 fois/jour", "1 fois/semaine", "1 fois/mois"],
        med_roots=["immuno", "auto", "cyto", "lympho", "sero", "inflammo"],
        med_suffixes=["mab", "cept", "trexate", "prine", "quine", "sone"],
        diseases=[
            "Syndrome de Sjögren",
            "Dermatomyosite",
            "Connectivite mixte",
            "Sclérose systémique diffuse",
            "Maladie de Still de l'adulte",
            "Sarcoïdose",
            "Myosite à inclusions",
            "Granulomatose avec polyangéite",
            "Polychondrite atrophiante",
            "Syndrome des antiphospholipides",
        ],
    ),
    CategoryConfig(
        name="Métabolisme et nutrition",
        source="synthetic_low_reference_metabo_v1",
        symptoms=[
            "fatigue", "prise de poids", "perte de poids", "fringales",
            "crampes", "chute de cheveux", "constipation", "diarrhée",
            "fourmillements", "faiblesse",
        ],
        secondary_symptoms=[
            "pâleur", "vertiges", "soif", "nausées", "troubles de concentration",
        ],
        advice=[
            "faire un bilan nutritionnel", "corriger les carences progressivement",
            "surveiller l'alimentation", "hydrater correctement", "éviter les régimes extrêmes",
        ],
        contraindications=[
            "surdosage vitaminique", "insuffisance rénale", "allergie aux compléments",
            "malabsorption sévère non évaluée",
        ],
        side_effects=[
            "nausées", "constipation", "diarrhée", "bouffées de chaleur", "céphalées",
        ],
        dosage_units=["1 mg", "5 mg", "10 mg", "500 mg", "1000 UI"],
        forms=["comprimé", "gélule", "ampoule buvable", "sachet"],
        frequencies=["1 fois/jour", "2 fois/jour", "1 fois/semaine"],
        med_roots=["metabo", "nutri", "vitro", "ferro", "calc", "mino"],
        med_suffixes=["fer", "calc", "vite", "zinc", "folate", "magnés"],
        diseases=[
            "Carence en fer",
            "Carence en vitamine D",
            "Carence en vitamine B12",
            "Obésité",
            "Dénutrition",
            "Syndrome métabolique",
            "Hypertriglycéridémie",
            "Hypercholestérolémie familiale",
            "Hypokaliémie",
            "Hypomagnésémie",
        ],
    ),
    CategoryConfig(
        name="Urologie",
        source="synthetic_low_reference_uro_v1",
        symptoms=[
            "brûlures urinaires", "envies fréquentes d'uriner", "douleur pelvienne",
            "sang dans les urines", "douleur lombaire", "jet faible", "fièvre",
            "urgence mictionnelle", "fuites urinaires", "douleur testiculaire",
        ],
        secondary_symptoms=[
            "urines troubles", "rétention urinaire", "frissons", "douleur périnéale", "pollakiurie",
        ],
        advice=[
            "boire suffisamment sauf contre-indication", "consulter rapidement en cas de fièvre",
            "ne pas retarder la prise en charge d'une rétention", "éviter les irritants urinaires",
            "surveiller la couleur des urines",
        ],
        contraindications=[
            "rétention aiguë d'urine", "insuffisance rénale sévère",
            "grossesse selon l'antibiotique", "glaucome selon l'anticholinergique",
        ],
        side_effects=[
            "sécheresse buccale", "vertiges", "nausées", "hypotension", "constipation",
        ],
        dosage_units=["5 mg", "10 mg", "250 mg", "500 mg", "1 g"],
        forms=["comprimé", "gélule", "solution injectable"],
        frequencies=["1 fois/jour", "2 fois/jour", "3 fois/jour"],
        med_roots=["uro", "vesico", "prosto", "reno", "cysto", "nephro"],
        med_suffixes=["floxacine", "xime", "zosine", "butynine", "tine", "pride"],
        diseases=[
            "Cystite aiguë",
            "Prostatite",
            "Hyperplasie bénigne de la prostate",
            "Incontinence urinaire d'effort",
            "Lithiase urinaire",
            "Épididymite",
            "Torsion testiculaire",
            "Cancer de la vessie",
            "Syndrome douloureux vésical",
            "Hydronéphrose",
        ],
    ),
    CategoryConfig(
        name="Hépatologie",
        source="synthetic_low_reference_hepato_v1",
        symptoms=[
            "jaunisse", "fatigue", "douleur de l'hypochondre droit", "nausées",
            "prurit", "urines foncées", "selles décolorées", "ballonnements",
            "ascite", "perte d'appétit",
        ],
        secondary_symptoms=[
            "œdèmes", "saignements", "confusion", "fièvre", "amaigrissement",
        ],
        advice=[
            "éviter l'alcool", "surveiller les signes de décompensation",
            "consulter rapidement en cas d'ictère", "respecter le suivi hépatique",
            "éviter les médicaments hépatotoxiques sans avis",
        ],
        contraindications=[
            "insuffisance hépatique avancée", "grossesse selon les antiviraux",
            "allergie au traitement", "déshydratation sévère",
        ],
        side_effects=[
            "nausées", "diarrhée", "céphalées", "fatigue", "rash",
        ],
        dosage_units=["50 mg", "100 mg", "250 mg", "500 mg", "600 mg"],
        forms=["comprimé", "gélule", "solution orale"],
        frequencies=["1 fois/jour", "2 fois/jour"],
        med_roots=["hepato", "bilio", "porto", "ictero", "fibro", "viralo"],
        med_suffixes=["vir", "mycine", "fibrate", "chol", "lase", "zole"],
        diseases=[
            "Hépatite B chronique",
            "Hépatite C chronique",
            "Stéatohépatite non alcoolique",
            "Cholangite",
            "Cholestase intrahépatique",
            "Encéphalopathie hépatique",
            "Hémochromatose",
            "Maladie de Wilson",
            "Cirrhose décompensée",
            "Syndrome de Budd-Chiari",
        ],
    ),
    CategoryConfig(
        name="Maladies vasculaires",
        source="synthetic_low_reference_vasculaire_v1",
        symptoms=[
            "douleur de jambe", "gonflement d'un membre", "rougeur locale",
            "sensation de chaleur", "fourmillements", "ulcère de jambe", "claudication",
            "extrémités froides", "changement de couleur des doigts", "douleur au repos",
        ],
        secondary_symptoms=[
            "fatigue", "crampes", "engourdissements", "lourdeurs", "cyanose",
        ],
        advice=[
            "consulter en urgence si douleur et gonflement brutaux", "surélever les jambes",
            "éviter l'immobilisation prolongée", "marcher régulièrement si possible",
            "porter une contention si indiquée",
        ],
        contraindications=[
            "saignement actif", "ulcère hémorragique", "grossesse selon l'anticoagulant",
            "insuffisance rénale sévère",
        ],
        side_effects=[
            "saignements", "ecchymoses", "vertiges", "nausées", "hypotension",
        ],
        dosage_units=["2,5 mg", "5 mg", "10 mg", "50 mg", "100 mg"],
        forms=["comprimé", "gel", "injection sous-cutanée"],
        frequencies=["1 fois/jour", "2 fois/jour"],
        med_roots=["vasculo", "phlebo", "arterio", "thrombo", "micro", "veino"],
        med_suffixes=["xaban", "parine", "prost", "lline", "dine", "azole"],
        diseases=[
            "Thrombose veineuse profonde",
            "Insuffisance veineuse chronique",
            "Artériopathie oblitérante des membres inférieurs",
            "Phlébite superficielle",
            "Syndrome de Raynaud",
            "Lymphœdème",
            "Anévrisme de l'aorte abdominale",
            "Dissection aortique",
            "Vascularite cutanée",
            "Ulcère veineux",
        ],
    ),
    CategoryConfig(
        name="Maladies tropicales et parasitaires",
        source="synthetic_low_reference_tropical_v1",
        symptoms=[
            "fièvre", "frissons", "douleurs musculaires", "fatigue", "diarrhée",
            "vomissements", "douleurs abdominales", "éruption", "sueurs", "anémie",
        ],
        secondary_symptoms=[
            "maux de tête", "ictère", "adénopathies", "prurit", "toux",
        ],
        advice=[
            "consulter rapidement après retour de zone tropicale", "s'hydrater abondamment",
            "surveiller la température", "ne pas retarder le bilan parasitaire",
            "signaler les voyages récents",
        ],
        contraindications=[
            "grossesse selon l'antiparasitaire", "allergie au traitement", "atteinte hépatique sévère",
            "allongement du QT selon la molécule",
        ],
        side_effects=[
            "nausées", "vertiges", "rash", "diarrhée", "céphalées",
        ],
        dosage_units=["100 mg", "200 mg", "250 mg", "500 mg", "600 mg"],
        forms=["comprimé", "gélule", "suspension orale"],
        frequencies=["1 fois/jour", "2 fois/jour", "3 jours de suite"],
        med_roots=["tropico", "parasito", "helmintho", "protozo", "malaro", "viralo"],
        med_suffixes=["quine", "azole", "mectine", "vir", "cycline", "mycine"],
        diseases=[
            "Amibiase intestinale",
            "Giardiase",
            "Schistosomiase",
            "Filariose lymphatique",
            "Onchocercose",
            "Trypanosomiase africaine",
            "Maladie de Chagas",
            "Leptospirose",
            "Rickettsiose",
            "Chikungunya",
        ],
    ),
    CategoryConfig(
        name="Génétique et maladies rares",
        source="synthetic_low_reference_rare_v1",
        symptoms=[
            "fatigue", "faiblesse musculaire", "retard de croissance", "douleurs diffuses",
            "troubles neurologiques", "déformations osseuses", "atteinte cutanée",
            "troubles visuels", "atteinte cardiaque", "atteinte respiratoire",
        ],
        secondary_symptoms=[
            "spasmes", "troubles digestifs", "amaigrissement", "crises", "intolérance à l'effort",
        ],
        advice=[
            "orienter vers un centre spécialisé", "documenter l'histoire familiale",
            "mettre en place un suivi multidisciplinaire", "surveiller les complications d'organes",
            "éviter l'automédication non validée",
        ],
        contraindications=[
            "insuffisance multiviscérale", "grossesse selon la molécule",
            "métabolisme hépatique complexe", "allergie à la molécule ciblée",
        ],
        side_effects=[
            "fatigue", "nausées", "céphalées", "troubles digestifs", "vertiges",
        ],
        dosage_units=["1 mg", "5 mg", "10 mg", "50 mg", "100 mg"],
        forms=["comprimé", "gélule", "solution buvable", "perfusion"],
        frequencies=["1 fois/jour", "2 fois/jour", "1 fois/semaine"],
        med_roots=["geno", "rare", "orpha", "lyso", "mito", "fabro"],
        med_suffixes=["zyme", "mab", "ase", "dine", "tide", "prine"],
        diseases=[
            "Maladie de Fabry",
            "Maladie de Gaucher",
            "Amyotrophie spinale",
            "Syndrome d'Ehlers-Danlos",
            "Porphyrie aiguë intermittente",
            "Maladie de Pompe",
            "Ataxie de Friedreich",
            "Syndrome d'Alport",
            "Neurofibromatose de type 1",
            "Syndrome de Marfan",
        ],
    ),
    CategoryConfig(
        name="Médecine interne",
        source="synthetic_low_reference_medinterne_v1",
        symptoms=[
            "fièvre prolongée", "fatigue", "amaigrissement", "douleurs diffuses",
            "éruption", "adénopathies", "transpiration nocturne", "arthralgies",
            "anomalies biologiques", "dyspnée",
        ],
        secondary_symptoms=[
            "céphalées", "nausées", "œdèmes", "prurit", "toux",
        ],
        advice=[
            "faire un bilan global approfondi", "documenter l'évolution des symptômes",
            "consulter sans tarder si aggravation systémique", "éviter l'automédication prolongée",
            "surveiller poids et température",
        ],
        contraindications=[
            "atteinte hépatique majeure", "insuffisance rénale sévère", "grossesse selon traitement",
            "infection active non contrôlée",
        ],
        side_effects=[
            "fatigue", "nausées", "diarrhée", "rash", "vertiges",
        ],
        dosage_units=["5 mg", "10 mg", "20 mg", "50 mg", "100 mg"],
        forms=["comprimé", "gélule", "perfusion"],
        frequencies=["1 fois/jour", "2 fois/jour", "1 fois/semaine"],
        med_roots=["interne", "systemo", "generalo", "multio", "interno", "polyo"],
        med_suffixes=["sone", "mab", "prine", "quine", "cept", "stat"],
        diseases=[
            "Fièvre d'origine indéterminée",
            "Amylose systémique",
            "Maladie de Castleman",
            "Syndrome de Gougerot",
            "Maladie de Horton",
            "Syndrome de Still",
            "Cryoglobulinémie mixte",
            "Fièvre méditerranéenne familiale",
            "Syndrome POEMS",
            "Histiocytose",
        ],
    ),
    CategoryConfig(
        name="Allergologie",
        source="synthetic_low_reference_allergo_v1",
        symptoms=[
            "éternuements", "nez qui coule", "démangeaisons", "urticaire",
            "gonflement", "sifflements", "toux", "yeux rouges", "eczéma", "oppression thoracique",
        ],
        secondary_symptoms=[
            "picotements", "larmoiement", "bouche qui gratte", "rougeur diffuse", "dyspnée",
        ],
        advice=[
            "éviter l'exposition à l'allergène suspect", "consulter en urgence si gonflement du visage",
            "garder un traitement de secours si prescrit", "aérer et nettoyer l'environnement",
            "documenter les déclencheurs",
        ],
        contraindications=[
            "allergie croisée connue", "glaucome pour certains antihistaminiques",
            "grossesse selon la molécule", "asthme non contrôlé",
        ],
        side_effects=[
            "somnolence", "bouche sèche", "vertiges", "nausées", "céphalées",
        ],
        dosage_units=["5 mg", "10 mg", "20 mg", "180 mg", "300 µg"],
        forms=["comprimé", "sirop", "spray nasal", "stylo injectable"],
        frequencies=["1 fois/jour", "2 fois/jour", "à la demande"],
        med_roots=["allergo", "histo", "pruri", "atopo", "respi", "anato"],
        med_suffixes=["dine", "zine", "mab", "phrine", "sone", "lukast"],
        diseases=[
            "Allergie saisonnière",
            "Allergie alimentaire",
            "Asthme allergique",
            "Anaphylaxie",
            "Œdème de Quincke",
            "Dermatite de contact allergique",
            "Allergie médicamenteuse",
            "Urticaire cholinergique",
            "Mastocytose systémique",
            "Allergie au latex",
        ],
    ),
    CategoryConfig(
        name="Stomatologie et médecine bucco-dentaire",
        source="synthetic_low_reference_dentaire_v1",
        symptoms=[
            "douleur dentaire", "gencive gonflée", "mauvaise haleine", "saignement gingival",
            "ulcération buccale", "sensibilité au chaud", "sensibilité au froid", "difficulté à mâcher",
            "gonflement de la joue", "fièvre",
        ],
        secondary_symptoms=[
            "trismus", "écoulement", "aphtes", "goût désagréable", "douleur faciale",
        ],
        advice=[
            "consulter un dentiste rapidement", "éviter les aliments très chauds ou très froids",
            "maintenir une bonne hygiène buccale", "consulter en urgence si gonflement facial important",
            "ne pas interrompre les soins dentaires nécessaires",
        ],
        contraindications=[
            "allergie aux antiseptiques", "grossesse selon les antalgiques", "insuffisance rénale selon l'antalgique",
            "ulcère digestif selon l'AINS",
        ],
        side_effects=[
            "nausées", "goût métallique", "somnolence", "vertiges", "irritation locale",
        ],
        dosage_units=["250 mg", "500 mg", "1 g", "0,12 %", "5 mg"],
        forms=["bain de bouche", "gel", "comprimé", "gélule"],
        frequencies=["2 fois/jour", "3 fois/jour", "application locale"],
        med_roots=["stomato", "dento", "gingivo", "ulcero", "oralo", "maxillo"],
        med_suffixes=["caïne", "mycine", "azole", "fen", "mol", "cilline"],
        diseases=[
            "Abcès dentaire",
            "Gingivite",
            "Parodontite",
            "Aphtose récidivante",
            "Péricoronarite",
            "Stomatite",
            "Candidose buccale",
            "Névralgie dentaire",
            "Alvéolite post-extraction",
            "Bruxisme douloureux",
        ],
    ),
    CategoryConfig(
        name="Médecine du sommeil",
        source="synthetic_low_reference_sommeil_v1",
        symptoms=[
            "insomnie", "somnolence diurne", "réveils nocturnes", "fatigue au réveil",
            "ronflements", "cauchemars", "agitation nocturne", "difficulté d'endormissement",
            "concentration difficile", "maux de tête matinaux",
        ],
        secondary_symptoms=[
            "irritabilité", "trous de mémoire", "anxiété", "palpitations", "sensation d'étouffement nocturne",
        ],
        advice=[
            "adopter une bonne hygiène de sommeil", "éviter les écrans le soir",
            "réduire caféine et alcool", "consulter si somnolence dangereuse", "garder des horaires réguliers",
        ],
        contraindications=[
            "association avec alcool", "grossesse selon les hypnotiques",
            "apnée du sommeil sévère non traitée", "insuffisance respiratoire",
        ],
        side_effects=[
            "somnolence diurne", "vertiges", "troubles de mémoire", "nausées", "bouche sèche",
        ],
        dosage_units=["1 mg", "3 mg", "5 mg", "10 mg", "25 mg"],
        forms=["comprimé", "gélule", "solution buvable"],
        frequencies=["au coucher", "1 fois/jour"],
        med_roots=["somno", "hypno", "veille", "nuito", "circa", "repos"],
        med_suffixes=["done", "lam", "zépam", "pram", "line", "xine"],
        diseases=[
            "Insomnie d'endormissement",
            "Insomnie de maintien",
            "Narcolepsie",
            "Syndrome des jambes sans repos",
            "Trouble du comportement en sommeil paradoxal",
            "Parasomnie",
            "Hypersomnie idiopathique",
            "Trouble du rythme circadien",
            "Bruxisme nocturne",
            "Terreurs nocturnes",
        ],
    ),
    CategoryConfig(
        name="Addictologie",
        source="synthetic_low_reference_addicto_v1",
        symptoms=[
            "envie irrépressible", "sevrage", "irritabilité", "tremblements",
            "anxiété", "insomnie", "nausées", "sueurs", "palpitations", "troubles de concentration",
        ],
        secondary_symptoms=[
            "agitation", "douleurs diffuses", "déprime", "fatigue", "crampes",
        ],
        advice=[
            "mettre en place un accompagnement structuré", "consulter sans délai en cas de sevrage sévère",
            "ne pas arrêter brutalement certaines substances", "associer soutien psychologique",
            "surveiller le risque de rechute",
        ],
        contraindications=[
            "insuffisance hépatique selon le traitement", "grossesse selon la molécule",
            "association à d'autres sédatifs", "allergie connue",
        ],
        side_effects=[
            "somnolence", "nausées", "vertiges", "céphalées", "fatigue",
        ],
        dosage_units=["2 mg", "8 mg", "25 mg", "50 mg", "150 mg"],
        forms=["comprimé", "gomme", "patch", "solution buvable"],
        frequencies=["1 fois/jour", "2 fois/jour", "à la demande encadrée"],
        med_roots=["addicto", "sevro", "tabaco", "alcoolo", "opio", "cravo"],
        med_suffixes=["xone", "fène", "prion", "line", "patch", "dine"],
        diseases=[
            "Dépendance au tabac",
            "Sevrage alcoolique",
            "Usage nocif de cannabis",
            "Dépendance aux opioïdes",
            "Dépendance aux benzodiazépines",
            "Usage problématique de cocaïne",
            "Trouble du jeu pathologique",
            "Addiction aux écrans",
            "Addiction aux achats",
            "Dépendance à la nicotine électronique",
        ],
    ),
]


SYMPTOM_SYNONYMS = {
    "douleur thoracique": ["gêne thoracique", "oppression dans la poitrine"],
    "essoufflement": ["dyspnée", "souffle court"],
    "fatigue": ["épuisement", "asthénie"],
    "vertiges": ["étourdissements", "sensation de tête qui tourne"],
    "fièvre": ["température élevée", "poussée fébrile"],
    "toux": ["quinte de toux", "toux persistante"],
    "nausées": ["envie de vomir", "haut-le-cœur"],
    "douleur abdominale": ["mal au ventre", "algie abdominale"],
    "maux de gorge": ["gorge douloureuse", "odynophagie"],
    "éruption cutanée": ["plaques cutanées", "rash cutané"],
    "douleurs articulaires": ["arthralgies", "articulations douloureuses"],
    "insomnie": ["difficulté à dormir", "sommeil perturbé"],
    "vision floue": ["vue brouillée", "baisse de netteté visuelle"],
    "brûlures urinaires": ["douleur en urinant", "miction brûlante"],
}


OPENERS = [
    "Le patient décrit",
    "Je présente",
    "Depuis quelques jours, j'ai",
    "Tableau clinique avec",
    "Le malade rapporte",
    "Consultation pour",
    "Symptomatologie dominée par",
    "Le parent signale",
]

INTENSITIES = [
    "modérés", "intenses", "progressifs", "persistants", "intermittents", "invalidants",
]

DURATIONS = [
    "depuis 24 heures", "depuis 3 jours", "depuis 1 semaine",
    "depuis 2 semaines", "depuis plusieurs mois",
]

CONTEXTS = [
    "aggravés à l'effort", "majorés la nuit", "avec retentissement sur les activités",
    "sans amélioration malgré le repos", "avec épisodes récurrents", "avec gêne au quotidien",
]

ADVICE_TEMPLATES = [
    "Conseils: {tips}.",
    "Conduite à tenir: {tips}.",
    "Mesures recommandées: {tips}.",
    "Avis initial: {tips}.",
]

SOURCE_VARIANTS = [
    "synthetic_low_reference_v1",
    "template_augmented_fr_v1",
    "light_reference_bootstrap_v1",
]


def slugify(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    return "".join(ch.lower() for ch in normalized if ch.isalnum())


def build_disease_records() -> list[dict]:
    diseases: list[dict] = []
    for category in CATEGORIES:
        for disease_name in category.diseases:
            diseases.append(
                {
                    "category": category.name,
                    "source": category.source,
                    "name": disease_name,
                    "symptoms": category.symptoms,
                    "secondary_symptoms": category.secondary_symptoms,
                    "advice": category.advice,
                    "contraindications": category.contraindications,
                    "side_effects": category.side_effects,
                    "dosage_units": category.dosage_units,
                    "forms": category.forms,
                    "frequencies": category.frequencies,
                    "med_roots": category.med_roots,
                    "med_suffixes": category.med_suffixes,
                }
            )
    return diseases


def maybe_synonym(rng: random.Random, symptom: str) -> str:
    variants = SYMPTOM_SYNONYMS.get(symptom)
    if variants and rng.random() < 0.45:
        return rng.choice(variants)
    return symptom


def build_symptom_sentence(rng: random.Random, disease: dict) -> str:
    core_count = rng.randint(3, 5)
    secondary_count = rng.randint(0, 2)

    core = rng.sample(disease["symptoms"], core_count)
    secondary = rng.sample(disease["secondary_symptoms"], secondary_count)
    symptoms = [maybe_synonym(rng, item) for item in core + secondary]
    rng.shuffle(symptoms)

    opener = rng.choice(OPENERS)
    intensity = rng.choice(INTENSITIES)
    duration = rng.choice(DURATIONS)
    context = rng.choice(CONTEXTS)

    if len(symptoms) == 1:
        joined = symptoms[0]
    elif len(symptoms) == 2:
        joined = f"{symptoms[0]} et {symptoms[1]}"
    else:
        joined = ", ".join(symptoms[:-1]) + f" et {symptoms[-1]}"

    sentence_templates = [
        f"{opener} {joined} {intensity} {duration}, {context}.",
        f"{opener.lower()} {joined}, symptômes {intensity} {duration}.",
        f"{joined.capitalize()} {duration}, avec tableau {intensity} {context}.",
        f"{opener} un ensemble de signes comprenant {joined} {duration}.",
    ]
    return rng.choice(sentence_templates)


def build_advice(rng: random.Random, disease: dict) -> str:
    tips = rng.sample(disease["advice"], 3)
    if rng.random() < 0.35:
        tips.append("consulter en urgence si aggravation brutale")
    return rng.choice(ADVICE_TEMPLATES).format(tips=", ".join(tips))


def generate_medication_catalog(diseases: list[dict]) -> tuple[list[dict], dict[str, list[dict]]]:
    catalog: list[dict] = []
    by_disease: dict[str, list[dict]] = {}
    med_counter = 1

    for disease_index, disease in enumerate(diseases, start=1):
        disease_slug = slugify(disease["name"])
        disease_code = f"D{disease_index:03d}"
        meds_for_disease: list[dict] = []

        for local_index in range(MEDICATIONS_PER_DISEASE):
            root = disease["med_roots"][local_index % len(disease["med_roots"])]
            suffix = disease["med_suffixes"][(local_index // len(disease["med_roots"])) % len(disease["med_suffixes"])]
            generic_name = f"{root}{disease_slug[:4]}{suffix}{local_index + 1:02d}"
            dosage = disease["dosage_units"][local_index % len(disease["dosage_units"])]
            form = disease["forms"][local_index % len(disease["forms"])]
            frequency = disease["frequencies"][local_index % len(disease["frequencies"])]
            price_tnd = round(3.5 + (disease_index * 0.07) + (local_index * 0.31), 3)

            medication = {
                "id": f"med_{med_counter:05d}",
                "name": f"{generic_name.capitalize()} {dosage} {form}",
                "generic_name": generic_name.capitalize(),
                "disease": disease["name"],
                "category": disease["category"],
                "form": form,
                "dosage": dosage,
                "frequency": frequency,
                "price_tnd": price_tnd,
                "indications": disease["symptoms"][:4],
                "contraindications": disease["contraindications"][:3],
                "side_effects": disease["side_effects"][:4],
                "source": disease["source"],
            }
            meds_for_disease.append(medication)
            catalog.append(medication)
            med_counter += 1

        by_disease[disease["name"]] = meds_for_disease

    return catalog, by_disease


def generate_dataset_rows(
    diseases: list[dict],
    medications_by_disease: dict[str, list[dict]],
) -> list[dict]:
    rows: list[dict] = []
    row_id = 1

    for disease_index, disease in enumerate(diseases, start=1):
        for sample_index in range(ROWS_PER_DISEASE):
            rng = random.Random(SEED + disease_index * 10_000 + sample_index)
            symptom_text = build_symptom_sentence(rng, disease)
            advice = build_advice(rng, disease)
            source = rng.choice([disease["source"], *SOURCE_VARIANTS])

            medication_pool = medications_by_disease[disease["name"]]
            med_count = rng.randint(3, 5)
            meds = rng.sample(medication_pool, med_count)
            recommended_medicines = " | ".join(med["name"] for med in meds)

            rows.append(
                {
                    "ID": row_id,
                    "symptoms": symptom_text,
                    "disease": disease["name"],
                    "recommended_medicines": recommended_medicines,
                    "advice": advice,
                    "source": source,
                }
            )
            row_id += 1

    return rows


def write_dataset(rows: list[dict]) -> None:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    with DATASET_PATH.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=["ID", "symptoms", "disease", "recommended_medicines", "advice", "source"],
        )
        writer.writeheader()
        writer.writerows(rows)


def write_catalog(catalog: list[dict]) -> None:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    with CATALOG_PATH.open("w", encoding="utf-8") as handle:
        json.dump(catalog, handle, ensure_ascii=False, indent=2)


def main() -> None:
    diseases = build_disease_records()
    catalog, medications_by_disease = generate_medication_catalog(diseases)
    rows = generate_dataset_rows(diseases, medications_by_disease)

    write_dataset(rows)
    write_catalog(catalog)

    print("Large dataset generation complete")
    print(f"- diseases: {len(diseases)}")
    print(f"- medications: {len(catalog)}")
    print(f"- rows: {len(rows)}")
    print(f"- dataset: {DATASET_PATH}")
    print(f"- catalog: {CATALOG_PATH}")


if __name__ == "__main__":
    main()
