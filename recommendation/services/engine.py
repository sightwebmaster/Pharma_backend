"""
services/engine.py
==================
Recommendation engine with multilingual DistilBERT, persistent vector store,
Constitutional AI rules, and RLHF re-ranking.
"""

import json
import logging
import os
import re
import time
from pathlib import Path
from typing import Optional

import numpy as np
import torch
from transformers import AutoModel, AutoTokenizer

from services.vector_store import PersistentVectorStore

logger = logging.getLogger(__name__)

URGENCY_PATTERNS = [
    r"chest\s+pain", r"heart\s+attack", r"cardiac\s+arrest",
    r"can.t\s+breathe", r"cannot\s+breathe", r"difficulty\s+breathing",
    r"seizure", r"convulsion", r"stroke",
    r"loss\s+of\s+consciousness", r"paralysis",
    r"high\s+fever\s+40", r"vomiting\s+blood", r"anaphylaxis",
    r"throat\s+swelling", r"overdose",
    r"douleur\s+thoracique", r"arr.t\s+cardiaque", r"convulsions",
    r"avc", r"perte\s+de\s+connaissance", r"paralysie",
    r"sang\s+dans\s+les\s+vomissements", r"gonflement\s+de\s+la\s+gorge",
]

PREGNANCY_WORDS = ["pregnant", "pregnancy", "enceinte", "grossesse", "trimester", "trimestre"]
PREGNANCY_BANNED = [
    "ibuprofen", "aspirin", "methotrexate", "warfarin", "rifampicin",
    "zolpidem", "diazepam", "codeine", "tramadol", "valproate", "carbamazepine",
    "fluconazole", "doxycycline", "tetracycline", "ciprofloxacin",
]
PEDIATRIC_BANNED = ["aspirin", "ibuprofen", "codeine", "tramadol", "zolpidem", "doxycycline"]
DANGEROUS_INTERACTIONS = [
    ("warfarin", "aspirin", "Major bleeding risk - dual antiplatelet/anticoagulant"),
    ("methotrexate", "ibuprofen", "Methotrexate toxicity increased"),
    ("clopidogrel", "omeprazole", "Reduced antiplatelet effect"),
    ("lithium", "ibuprofen", "Lithium toxicity risk"),
    ("valproate", "carbamazepine", "Pharmacokinetic interaction"),
    ("zolpidem", "lorazepam", "CNS and respiratory depression"),
    ("nitroglycerin", "amlodipine", "Severe hypotension risk"),
    ("digoxin", "amiodarone", "Digoxin toxicity increased"),
    ("metformin", "furosemide", "Lactic acidosis risk with dehydration"),
]

FINETUNED_PATH = "model_finetuned"
BASE_MODEL = os.getenv("RECOMMENDATION_BASE_MODEL", "distilbert-base-multilingual-cased")
VECTOR_STORE_DIR = Path(os.getenv("VECTOR_STORE_DIR", str(Path("data") / "vector_store")))


class RecommendationEngine:
    def __init__(self):
        self.tokenizer = None
        self.model = None
        self.meds: list[dict] = []
        self.ready = False
        self.model_path: Optional[str] = None
        self.disease_to_meds: dict = {}
        self.label_map: dict = {}
        self.vector_store = PersistentVectorStore(VECTOR_STORE_DIR)
        self._urgency_re = [re.compile(pattern, re.I) for pattern in URGENCY_PATTERNS]

    def load_model(self):
        if os.path.exists(FINETUNED_PATH) and os.path.exists(f"{FINETUNED_PATH}/config.json"):
            self.model_path = FINETUNED_PATH
            logger.info("Fine-tuned multilingual model found -> %s", FINETUNED_PATH)
            for fname, attr in [
                ("meta.json", None),
                ("disease_to_meds.json", "disease_to_meds"),
                ("label_map.json", "label_map"),
            ]:
                fpath = f"{FINETUNED_PATH}/{fname}"
                if os.path.exists(fpath):
                    with open(fpath, encoding="utf-8") as handle:
                        data = json.load(handle)
                    if attr:
                        setattr(self, attr, data)
                    else:
                        logger.info(
                            "Accuracy=%s%% classes=%s target=%s language=%s",
                            data.get("best_val_accuracy"),
                            data.get("nb_classes"),
                            data.get("target"),
                            data.get("language_profile", "unknown"),
                        )
        else:
            self.model_path = BASE_MODEL
            logger.warning(
                "Fine-tuned model not found; using multilingual base model %s",
                BASE_MODEL,
            )

        started_at = time.time()
        self.tokenizer = AutoTokenizer.from_pretrained(self.model_path)
        self.model = AutoModel.from_pretrained(self.model_path)
        self.model.eval()
        logger.info("Model loaded in %.1fs", time.time() - started_at)

    def _encode(self, text: str) -> np.ndarray:
        inputs = self.tokenizer(
            text,
            return_tensors="pt",
            truncation=True,
            max_length=160,
            padding=True,
        )
        with torch.no_grad():
            outputs = self.model(**inputs)

        token_embeddings = outputs.last_hidden_state
        attention_mask = inputs["attention_mask"]
        mask_expanded = attention_mask.unsqueeze(-1).float()
        pooled = (token_embeddings * mask_expanded).sum(dim=1) / mask_expanded.sum(dim=1).clamp(min=1e-9)
        vector = pooled.squeeze(0).cpu().numpy().astype(np.float32)
        norm = np.linalg.norm(vector)
        return vector / norm if norm > 1e-9 else vector

    @staticmethod
    def _medication_text(medication: dict) -> str:
        indications = " ".join(medication.get("indications", []))
        contraindications = " ".join(medication.get("contraindications", []))
        side_effects = " ".join(medication.get("side_effects", []))
        return (
            f"{medication.get('name', '')}. "
            f"Principe actif: {medication.get('dci', '')}. "
            f"Categorie: {medication.get('category', '')}. "
            f"Maladie: {medication.get('disease', '')}. "
            f"Indications: {indications}. "
            f"Contre indications: {contraindications}. "
            f"Effets secondaires: {side_effects}. "
            f"Description: {medication.get('description', '')}"
        ).strip()

    async def build_index(self, medications: list[dict], force_rebuild: bool = False):
        self.meds = medications
        if not medications:
            self.ready = False
            logger.warning("No medications available to build vector store")
            return

        fingerprint = self.vector_store.build_fingerprint(medications)
        if not force_rebuild and self.vector_store.load(expected_fingerprint=fingerprint):
            self.meds = self.vector_store.metadata
            self.ready = True
            logger.info("Reused persistent vector store backend=%s", self.vector_store.backend)
            return

        vectors = []
        for medication in medications:
            vectors.append(self._encode(self._medication_text(medication)))

        matrix = np.vstack(vectors).astype(np.float32)
        self.vector_store.save(matrix, medications, fingerprint)
        self.ready = True
        logger.info(
            "Vector store rebuilt with %s medications via %s",
            len(medications),
            self.vector_store.backend,
        )

    def analyze(
        self,
        symptoms: str,
        patient_profile: Optional[dict] = None,
        rlhf_scores: Optional[dict] = None,
        top_k: int = 5,
    ) -> dict:
        started_at = time.time()
        symptoms_lower = symptoms.lower().strip()

        for pattern in self._urgency_re:
            if pattern.search(symptoms_lower):
                return {
                    "status": "EMERGENCY",
                    "message": (
                        "Emergency symptoms detected. Call emergency services immediately "
                        "or go to the nearest hospital."
                    ),
                    "symptoms": symptoms,
                    "recommended_medications": [],
                    "constitutional_rule": "R8",
                    "redirect_to_doctor": True,
                    "requires_pharmacist_validation": False,
                    "latency_ms": round((time.time() - started_at) * 1000, 1),
                }

        query_vector = self._encode(symptoms)
        nearest = self.vector_store.search(query_vector, max(top_k * 3, 12))

        candidates = []
        for idx, score in nearest:
            if score < 0.08 or idx >= len(self.meds):
                continue
            medication = self.meds[idx].copy()
            medication["similarity_score"] = round(float(score), 4)
            medication["score_label"] = self._score_label(float(score))
            candidates.append(medication)

        if self.disease_to_meds and candidates:
            top_disease = candidates[0].get("disease", "")
            if top_disease in self.disease_to_meds:
                known_meds = self.disease_to_meds[top_disease].get("medicine", "")
                for medication in candidates:
                    if any(
                        known.strip().lower() in medication.get("name", "").lower()
                        for known in known_meds.split("|")
                    ):
                        medication["similarity_score"] = min(1.0, medication["similarity_score"] + 0.06)
                        medication["dataset_match"] = True

        profile = patient_profile or {}
        allergies = [str(item).lower() for item in profile.get("allergies", [])]
        conditions = [str(item).lower() for item in profile.get("conditions", [])]
        is_pregnant = bool(profile.get("pregnant", False)) or any(word in symptoms_lower for word in PREGNANCY_WORDS)
        is_pediatric = self._is_pediatric(symptoms_lower, profile)

        violations, alerts, warnings, filtered = [], [], [], []

        for medication in candidates:
            name_lower = medication.get("name", "").lower()
            dci_lower = medication.get("dci", "").lower()
            keep = True

            if medication.get("prescription_required"):
                alerts.append(f"R1 - '{medication['name']}': prescription required.")

            for allergy in allergies:
                if allergy in name_lower or allergy in dci_lower:
                    violations.append(f"R2 - '{medication['name']}' excluded: allergy '{allergy}'")
                    keep = False
                    break

            if keep and is_pregnant:
                for banned in PREGNANCY_BANNED:
                    if banned in name_lower or banned in dci_lower:
                        violations.append(f"R4 - '{medication['name']}' excluded: pregnancy contraindication")
                        warnings.append("Pregnancy risk detected. Potentially dangerous medications excluded.")
                        keep = False
                        break

            if keep and is_pediatric:
                for banned in PEDIATRIC_BANNED:
                    if banned in name_lower or banned in dci_lower:
                        alerts.append(f"R6 - '{medication['name']}': check pediatric dosage.")
                        break

            if keep and any("renal" in condition or "kidney" in condition or "rein" in condition for condition in conditions):
                for risky in ["ibuprofen", "naproxen", "metformin", "gentamicin", "vancomycin"]:
                    if risky in name_lower or risky in dci_lower:
                        alerts.append(f"R7 - '{medication['name']}': dose adjustment required in renal impairment.")

            if keep:
                filtered.append(medication)

        filtered_names = [medication.get("name", "").lower() for medication in filtered]
        for drug_a, drug_b, risk in DANGEROUS_INTERACTIONS:
            if any(drug_a in name for name in filtered_names) and any(drug_b in name for name in filtered_names):
                violations.append(f"R3 - {drug_a} <-> {drug_b}: {risk}")
                alerts.append(f"Interaction alert: {risk}")

        if len(filtered) > 3:
            violations.append("R5 - List limited to 3 medications (polypharmacy rule).")
            filtered = filtered[:3]

        warnings.append("AI recommendations require pharmacist validation before dispensing.")

        for medication in filtered:
            bonus = float((rlhf_scores or {}).get(medication.get("name", ""), 0.0))
            medication["rlhf_bonus"] = round(bonus, 4)
            medication["final_score"] = round(
                min(1.0, max(0.0, medication["similarity_score"] + bonus * 0.3)),
                4,
            )

        filtered = sorted(filtered, key=lambda item: item["final_score"], reverse=True)
        is_fine_tuned = self.model_path == FINETUNED_PATH
        primary_disease = filtered[0].get("disease", "undetermined") if filtered else "undetermined"
        dataset_advice = self.disease_to_meds.get(primary_disease, {}).get("advice", "") if self.disease_to_meds else ""

        return {
            "status": "OK",
            "symptoms": symptoms,
            "primary_disease": primary_disease,
            "dataset_advice": dataset_advice,
            "model_type": "fine-tuned multilingual" if is_fine_tuned else "multilingual base",
            "top_score": filtered[0]["final_score"] if filtered else 0.0,
            "confidence_label": self._score_label(filtered[0]["final_score"] if filtered else 0.0),
            "recommended_medications": filtered,
            "medications_found": len(filtered),
            "constitutional_violations": violations,
            "pharmacist_alerts": list(dict.fromkeys(alerts)),
            "mandatory_warnings": list(dict.fromkeys(warnings)),
            "redirect_to_doctor": False,
            "requires_pharmacist_validation": True,
            "model": self.model_path,
            "vector_store_backend": self.vector_store.backend,
            "latency_ms": round((time.time() - started_at) * 1000, 1),
        }

    def get_status(self) -> dict:
        is_fine_tuned = self.model_path == FINETUNED_PATH
        meta = {}
        if is_fine_tuned and os.path.exists(f"{FINETUNED_PATH}/meta.json"):
            with open(f"{FINETUNED_PATH}/meta.json", encoding="utf-8") as handle:
                meta = json.load(handle)
        return {
            "ready": self.ready,
            "model_path": self.model_path,
            "is_finetuned": is_fine_tuned,
            "embedding_dims": 768,
            "medications_indexed": len(self.meds),
            "diseases_mapped": len(self.disease_to_meds),
            "vector_store_backend": self.vector_store.backend,
            "vector_store_path": str(self.vector_store.base_dir),
            "training_accuracy": meta.get("best_val_accuracy", "N/A"),
            "trained_on": meta.get("nb_classes", "N/A"),
            "dataset": meta.get("dataset", "N/A"),
            "language_profile": meta.get("language_profile", "multilingual"),
            "train_samples": meta.get("train_samples", "N/A"),
            "val_samples": meta.get("val_samples", "N/A"),
        }

    def _is_pediatric(self, symptoms_lower: str, profile: dict) -> bool:
        age = profile.get("age", 99)
        if isinstance(age, (int, float)) and age < 12:
            return True
        match = re.search(r"(child|enfant|kid|baby|bébé).{0,20}(\d{1,2})\s*(year|an|ans)", symptoms_lower)
        return bool(match and int(match.group(2)) < 12)

    @staticmethod
    def _score_label(score: float) -> str:
        if score >= 0.75:
            return "HIGH"
        if score >= 0.50:
            return "MEDIUM"
        if score >= 0.25:
            return "LOW"
        return "VERY LOW"
