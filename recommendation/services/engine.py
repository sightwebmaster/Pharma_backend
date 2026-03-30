"""
services/engine.py
═══════════════════════════════════════════════════════════════
Moteur de recommandation — utilise le modèle fine-tuné

Différence avec la version sans training :
  AVANT → distilbert-base-multilingual-cased (générique)
  APRÈS → ./model_finetuned (spécialisé médical)

Le modèle fine-tuné connaît directement nos médicaments.
Les vecteurs produits sont beaucoup plus précis.
"""

import re, os, time, json, logging
import numpy as np
import torch
from transformers import DistilBertTokenizer, DistilBertModel
from sklearn.metrics.pairwise import cosine_similarity
from typing import Optional

logger = logging.getLogger(__name__)

# ─────────────────────────────────────────────────────────────
# RÈGLES CONSTITUTIONAL AI
# ─────────────────────────────────────────────────────────────

URGENCY_WORDS = [
    "douleur thoracique", "douleur poitrine", "infarctus", "crise cardiaque",
    "difficulté à respirer", "perte de connaissance", "convulsion",
    "paralysie", "avc", "accident vasculaire",
    "vomissement de sang", "sang dans les selles",
    "choc anaphylactique", "gonflement gorge",
    "fièvre 40", "40 degrés", "overdose", "surdosage",
]

PREGNANCY_WORDS  = ["enceinte", "grossesse", "trimestre"]
PREGNANCY_BANNED = [
    "ibuprofène", "aspirine", "sumatriptan",
    "diazépam", "tramadol", "codéine",
    "ciprofloxacine", "prednisolone",
]
PEDIATRIC_RISK  = ["ibuprofène", "aspirine", "codéine", "tramadol", "diazépam"]
DANGEROUS_PAIRS = [
    ("ibuprofène", "aspirine",   "Double AINS — risque hémorragique"),
    ("tramadol",   "diazépam",   "Dépression respiratoire"),
    ("tramadol",   "codéine",    "Surdosage opioïde"),
    ("metformine", "furosémide", "Risque acidose lactique"),
]

# Chemin du modèle fine-tuné
FINETUNED_PATH = "model_finetuned"
BASE_MODEL     = "distilbert-base-multilingual-cased"

# ─────────────────────────────────────────────────────────────
# MOTEUR
# ─────────────────────────────────────────────────────────────

class RecommendationEngine:
    """
    Utilise le modèle fine-tuné si disponible,
    sinon utilise DistilBERT de base.

    Avantage du modèle fine-tuné :
      - Vecteurs spécialisés domaine médical
      - Meilleure précision scores RAG (~+15%)
      - Comprend les termes médicaux spécifiques
    """

    def __init__(self):
        self.tokenizer  = None
        self.model      = None
        self.meds       : list[dict]           = []
        self.embeddings : Optional[np.ndarray] = None
        self.ready      = False
        self.model_path = None

    # ── Chargement ────────────────────────────────────────────

    def load_model(self):
        """
        Charge le modèle fine-tuné si disponible,
        sinon charge DistilBERT de base.
        """
        # Priorité 1 : modèle fine-tuné
        if os.path.exists(FINETUNED_PATH) and \
           os.path.exists(f"{FINETUNED_PATH}/config.json"):

            self.model_path = FINETUNED_PATH
            logger.info(f"✅ Modèle fine-tuné trouvé → {FINETUNED_PATH}")

            # Charger les métadonnées
            meta_path = f"{FINETUNED_PATH}/meta.json"
            if os.path.exists(meta_path):
                with open(meta_path, encoding="utf-8") as f:
                    meta = json.load(f)
                logger.info(
                    f"   Précision validation : {meta.get('best_val_accuracy')}%"
                    f" | {meta.get('nb_classes')} médicaments"
                )
        else:
            # Priorité 2 : modèle de base
            self.model_path = BASE_MODEL
            logger.warning(
                f"⚠️  Modèle fine-tuné non trouvé dans '{FINETUNED_PATH}'\n"
                f"   → Utilisation de DistilBERT de base\n"
                f"   → Lancer 'python train.py' pour fine-tuner"
            )

        t0 = time.time()
        logger.info(f"⏳ Chargement modèle : {self.model_path}")

        self.tokenizer = DistilBertTokenizer.from_pretrained(self.model_path)
        self.model     = DistilBertModel.from_pretrained(self.model_path)
        self.model.eval()

        logger.info(f"✅ Modèle chargé en {time.time()-t0:.1f}s")

    def _encode(self, text: str) -> np.ndarray:
        """
        Encode un texte → vecteur 768 dimensions.

        Avec modèle fine-tuné :
          → vecteurs enrichis par la connaissance médicale
          → "fièvre" très proche de "Paracétamol"

        Avec modèle de base :
          → vecteurs génériques
          → "fièvre" proche de "chaleur", "température"
        """
        inputs = self.tokenizer(
            text,
            return_tensors="pt",
            truncation=True,
            max_length=128,
            padding=True,
        )
        with torch.no_grad():
            outputs = self.model(**inputs)

        # Mean pooling
        token_emb = outputs.last_hidden_state           # (1, T, 768)
        mask      = inputs["attention_mask"]             # (1, T)
        mask_exp  = mask.unsqueeze(-1).float()           # (1, T, 1)
        sum_emb   = (token_emb * mask_exp).sum(dim=1)
        sum_mask  = mask_exp.sum(dim=1).clamp(min=1e-9)
        vec       = (sum_emb / sum_mask).squeeze(0).numpy()

        # Normalisation L2
        norm = np.linalg.norm(vec)
        return vec / norm if norm > 0 else vec

    async def build_index(self, medications: list[dict]):
        """Encode tous les médicaments → index RAG."""
        self.meds   = medications
        vectors     = []

        for m in medications:
            inds = ", ".join(m.get("indications", []))
            text = (
                f"{m.get('name','')} {m.get('dci','')} "
                f"{m.get('category','')} indications: {inds} "
                f"{m.get('description','')}"
            )
            vectors.append(self._encode(text))

        self.embeddings = np.vstack(vectors)
        self.ready      = True
        logger.info(
            f"🔨 Index RAG : {self.embeddings.shape[0]} médicaments "
            f"× {self.embeddings.shape[1]} dimensions"
            f" | modèle : {'fine-tuné ✅' if self.model_path == FINETUNED_PATH else 'base ⚠️'}"
        )

    # ── Pipeline principal ────────────────────────────────────

    def analyze(self, symptoms: str, patient_profile: Optional[dict] = None,
                rlhf_scores: Optional[dict] = None, top_k: int = 5) -> dict:
        """Pipeline complet — 5 étapes."""

        t0 = time.time()
        sl = symptoms.lower()

        # ÉTAPE 1 — Constitutional AI R8
        for kw in URGENCY_WORDS:
            if kw in sl:
                return {
                    "status":   "URGENCE",
                    "message":  "🚨 Symptômes d'urgence. Consultez un médecin ou appelez le 15.",
                    "symptoms": symptoms,
                    "recommended_medications":       [],
                    "constitutional_rule":           "R8",
                    "redirect_to_doctor":            True,
                    "requires_pharmacist_validation": False,
                    "latency_ms": round((time.time()-t0)*1000, 1),
                }

        # ÉTAPE 2 — DistilBERT encode les symptômes
        query_vec = self._encode(symptoms).reshape(1, -1)

        # ÉTAPE 3 — RAG similarité cosine
        scores    = cosine_similarity(query_vec, self.embeddings)[0]
        top_idx   = np.argsort(scores)[::-1][:top_k * 2]

        candidates = []
        for i in top_idx:
            sc = float(scores[i])
            if sc < 0.10:
                continue
            m = self.meds[i].copy()
            m["similarity_score"] = round(sc, 4)
            m["score_label"]      = self._label(sc)
            candidates.append(m)

        # ÉTAPE 4 — Constitutional AI filtrage
        profile    = patient_profile or {}
        allergies  = [a.lower() for a in profile.get("allergies",  [])]
        conditions = [c.lower() for c in profile.get("conditions", [])]
        is_pregnant  = profile.get("pregnant", False) or \
                       any(kw in sl for kw in PREGNANCY_WORDS)
        is_pediatric = self._is_pediatric(sl, profile)

        violations, alerts, warnings, filtered = [], [], [], []

        for m in candidates:
            name_l = m.get("name", "").lower()
            dci_l  = m.get("dci",  "").lower()
            keep   = True

            if m.get("prescription_required"):
                alerts.append(f"R1 — '{m['name']}' : ordonnance obligatoire.")

            for al in allergies:
                if al in name_l or al in dci_l:
                    violations.append(f"R2 — '{m['name']}' retiré : allergie '{al}'")
                    keep = False; break

            if keep and is_pregnant:
                for b in PREGNANCY_BANNED:
                    if b in name_l or b in dci_l:
                        violations.append(f"R4 — '{m['name']}' retiré : grossesse")
                        warnings.append("⚠️ Grossesse — médicaments dangereux exclus.")
                        keep = False; break

            if keep and is_pediatric:
                for r in PEDIATRIC_RISK:
                    if r in name_l or r in dci_l:
                        alerts.append(f"R6 — '{m['name']}' : posologie pédiatrique à vérifier.")
                        break

            if keep and "insuffisance rénale" in conditions:
                for r in ["ibuprofène", "fosfomycine", "metformine", "furosémide"]:
                    if r in name_l or r in dci_l:
                        alerts.append(f"R7 — '{m['name']}' : ajustement dose requis.")
                        break

            if keep:
                filtered.append(m)

        names_l = [m.get("name", "").lower() for m in filtered]
        for a, b, risk in DANGEROUS_PAIRS:
            if any(a in n for n in names_l) and any(b in n for n in names_l):
                violations.append(f"R3 — {a} ↔ {b} : {risk}")
                alerts.append(f"⚠️ Interaction : {risk}")

        if len(filtered) > 3:
            violations.append("R5 — Liste réduite à 3 médicaments.")
            filtered = filtered[:3]

        warnings.append(
            "ℹ️ Recommandations IA — validation pharmacien obligatoire."
        )

        # ÉTAPE 5 — RLHF ajustement scores
        if rlhf_scores:
            for m in filtered:
                bonus = rlhf_scores.get(m.get("name", ""), 0.0)
                m["rlhf_bonus"]  = round(bonus, 4)
                m["final_score"] = round(
                    min(1.0, max(0.0, m["similarity_score"] + bonus * 0.3)), 4
                )
            filtered = sorted(
                filtered, key=lambda x: x.get("final_score", 0), reverse=True
            )
        else:
            for m in filtered:
                m["rlhf_bonus"]  = 0.0
                m["final_score"] = m["similarity_score"]

        is_finetuned = self.model_path == FINETUNED_PATH

        return {
            "status":    "OK",
            "symptoms":  symptoms,
            "language":  "fr",
            "model_type": "fine-tuned ✅" if is_finetuned else "base ⚠️",
            "primary_category":          filtered[0]["category"] if filtered else "indéterminé",
            "top_score":                 filtered[0]["final_score"] if filtered else 0.0,
            "confidence_label":          self._label(filtered[0]["final_score"] if filtered else 0.0),
            "recommended_medications":   filtered,
            "medications_found":         len(filtered),
            "constitutional_violations": violations,
            "pharmacist_alerts":         list(set(alerts)),
            "mandatory_warnings":        list(set(warnings)),
            "redirect_to_doctor":        False,
            "requires_pharmacist_validation": True,
            "model":      self.model_path,
            "latency_ms": round((time.time()-t0)*1000, 1),
        }

    def get_status(self) -> dict:
        is_finetuned = self.model_path == FINETUNED_PATH
        meta = {}
        if is_finetuned and os.path.exists(f"{FINETUNED_PATH}/meta.json"):
            with open(f"{FINETUNED_PATH}/meta.json", encoding="utf-8") as f:
                meta = json.load(f)
        return {
            "ready":               self.ready,
            "model_path":          self.model_path,
            "is_finetuned":        is_finetuned,
            "embedding_dims":      768,
            "medications_indexed": len(self.meds),
            "training_accuracy":   meta.get("best_val_accuracy", "N/A"),
            "trained_on":          meta.get("nb_classes", "N/A"),
        }

    def _is_pediatric(self, sl: str, profile: dict) -> bool:
        age = profile.get("age", 99)
        if isinstance(age, int) and age < 12:
            return True
        m = re.search(r'(enfant|fils|fille|bébé).{0,20}(\d{1,2})\s*ans', sl)
        return bool(m and int(m.group(2)) < 12)

    @staticmethod
    def _label(score: float) -> str:
        if score >= 0.70: return "🟢 ÉLEVÉE"
        if score >= 0.45: return "🟡 MOYENNE"
        if score >= 0.20: return "🟠 FAIBLE"
        return "🔴 TRÈS FAIBLE"
