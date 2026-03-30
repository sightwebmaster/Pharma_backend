"""
routers/router.py
═════════════════════════════════════════════════════
Rôle : Définit tous les endpoints REST de l'API

Groupes d'endpoints :
  /recommendations  → analyser, valider, historique
  /catalog          → voir, ajouter, modifier médicaments
  /rlhf             → dashboard, scores
  /system           → santé, statut, rebuild RAG
"""

from fastapi import APIRouter, Request, HTTPException
from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime
from db.mongodb import RecommendationRepo, MedicationRepo, RLHFRepo

router    = APIRouter()
rec_repo  = RecommendationRepo()
med_repo  = MedicationRepo()
rlhf_repo = RLHFRepo()

# ─────────────────────────────────────────────────────────────────────────────
# SCHÉMAS (validation des données entrantes)
# ─────────────────────────────────────────────────────────────────────────────

class AnalyzeRequest(BaseModel):
    symptoms:        str  = Field(..., min_length=3,
                                  description="Symptômes en français ou arabe",
                                  examples=["j'ai de la fièvre et des maux de tête"])
    patient_id:      str  = Field(default="anonymous")
    top_k:           int  = Field(default=5, ge=1, le=10)
    patient_profile: Optional[dict] = Field(
        default=None,
        description="Profil patient (optionnel)",
        examples=[{
            "age": 30,
            "allergies": ["pénicilline"],
            "conditions": [],
            "pregnant": False
        }]
    )

class ValidateRequest(BaseModel):
    pharmacist_id:        str  = Field(default="pharmacist-1")
    action:               str  = Field(..., pattern="^(VALIDATE|REJECT|MODIFY)$",
                                       description="VALIDATE / REJECT / MODIFY")
    pharmacist_note:      Optional[str]  = None
    modified_medications: Optional[list] = Field(
        default=None,
        description="Liste modifiée (seulement si action=MODIFY)"
    )

class AddMedicationRequest(BaseModel):
    id:                   str
    name:                 str
    dci:                  str
    category:             str
    dosage:               str
    frequency:            str
    indications:          list[str]
    contraindications:    list[str]
    side_effects:         list[str]
    prescription_required: bool
    price_tnd:            float
    description:          str

# ─────────────────────────────────────────────────────────────────────────────
# GROUPE 1 : RECOMMANDATIONS
# ─────────────────────────────────────────────────────────────────────────────

@router.post(
    "/recommendations/analyze",
    summary="🧠 Analyser des symptômes",
    tags=["Recommandations"],
    status_code=201,
)
async def analyze(body: AnalyzeRequest, request: Request):
    """
    Pipeline complet :
    1. Constitutional AI vérifie les urgences
    2. DistilBERT encode les symptômes
    3. RAG cherche dans les 50 médicaments
    4. Constitutional AI filtre (allergies, grossesse...)
    5. RLHF ajuste les scores
    6. Sauvegarde dans MongoDB avec status PENDING
    """
    engine      = request.app.state.engine
    rlhf_scores = await rlhf_repo.get_scores()

    # Lancer le pipeline
    result = engine.analyze(
        symptoms        = body.symptoms,
        patient_profile = body.patient_profile,
        rlhf_scores     = rlhf_scores,
        top_k           = body.top_k,
    )

    # Sauvegarder dans MongoDB
    doc_id = await rec_repo.create({
        "patient_id":      body.patient_id,
        "patient_profile": body.patient_profile,
        **result,
    })

    return await rec_repo.find_by_id(doc_id)


@router.get(
    "/recommendations/pending",
    summary="📋 Recommandations en attente (pharmacien)",
    tags=["Recommandations"],
)
async def get_pending():
    """Retourne toutes les recommandations avec status PENDING."""
    return await rec_repo.find_pending()


@router.get(
    "/recommendations/patient/{patient_id}",
    summary="📜 Historique d'un patient",
    tags=["Recommandations"],
)
async def get_patient_history(patient_id: str):
    """Retourne toutes les recommandations d'un patient donné."""
    return await rec_repo.find_by_patient(patient_id)


@router.get(
    "/recommendations/{rec_id}",
    summary="🔍 Lire une recommandation",
    tags=["Recommandations"],
)
async def get_recommendation(rec_id: str):
    """Cherche une recommandation par son ID MongoDB."""
    doc = await rec_repo.find_by_id(rec_id)
    if not doc:
        raise HTTPException(404, "Recommandation introuvable")
    return doc


@router.put(
    "/recommendations/{rec_id}/validate",
    summary="✅ Valider / Modifier / Rejeter",
    tags=["Recommandations"],
)
async def validate(rec_id: str, body: ValidateRequest):
    """
    Le pharmacien prend sa décision :
    - VALIDATE : approuve tel quel
    - MODIFY   : modifie la liste de médicaments
    - REJECT   : rejette complètement

    Génère automatiquement un signal RLHF enregistré dans MongoDB.
    """
    original = await rec_repo.find_by_id(rec_id)
    if not original:
        raise HTTPException(404, "Recommandation introuvable")

    # Mettre à jour dans MongoDB
    ok = await rec_repo.validate(
        rec_id, body.pharmacist_id, body.action,
        body.pharmacist_note, body.modified_medications,
    )
    if not ok:
        raise HTTPException(400, "Déjà traitée ou introuvable")

    # ── Calculer et sauvegarder le signal RLHF ────────────────────────────
    orig_meds  = {m.get("name", "") for m in original.get("recommended_medications", [])}
    final_meds = set()

    if body.action == "VALIDATE":
        final_meds = orig_meds
    elif body.action == "MODIFY" and body.modified_medications:
        final_meds = {m.get("name", "") for m in body.modified_medications}

    removed    = list(orig_meds - final_meds)
    added      = list(final_meds - orig_meds)
    agreement  = len(orig_meds & final_meds) / len(orig_meds) if orig_meds else 1.0

    rlhf_signal = (
        agreement       if body.action == "VALIDATE" else
        agreement - 0.5 if body.action == "MODIFY"   else
        -1.0
    )

    await rlhf_repo.save({
        "recommendation_id":     rec_id,
        "patient_id":            original.get("patient_id", ""),
        "pharmacist_id":         body.pharmacist_id,
        "action":                body.action,
        "symptoms":              original.get("symptoms", ""),
        "original_medications":  list(orig_meds),
        "final_medications":     list(final_meds),
        "removed_by_pharmacist": removed,
        "added_by_pharmacist":   added,
        "agreement_rate":        round(agreement, 4),
        "rlhf_signal":           round(rlhf_signal, 4),
        "pharmacist_note":       body.pharmacist_note,
    })

    return await rec_repo.find_by_id(rec_id)


# ─────────────────────────────────────────────────────────────────────────────
# GROUPE 2 : CATALOGUE MÉDICAMENTS
# ─────────────────────────────────────────────────────────────────────────────

@router.get(
    "/catalog",
    summary="📚 Tout le catalogue",
    tags=["Catalogue"],
)
async def list_catalog(
    category:     Optional[str]  = None,
    prescription: Optional[bool] = None,
):
    """
    Retourne tous les médicaments.
    Filtres optionnels : category, prescription_required
    """
    meds = await med_repo.get_all()
    if category:
        meds = [m for m in meds if category.lower() in m.get("category", "").lower()]
    if prescription is not None:
        meds = [m for m in meds if m.get("prescription_required") == prescription]
    return {"count": len(meds), "medications": meds}


@router.get(
    "/catalog/search",
    summary="🔍 Rechercher un médicament",
    tags=["Catalogue"],
)
async def search_catalog(q: str):
    """Recherche textuelle dans le catalogue (nom, indications, description)."""
    results = await med_repo.search_text(q, limit=10)
    return {"count": len(results), "results": results}


@router.get(
    "/catalog/{med_id}",
    summary="💊 Détail d'un médicament",
    tags=["Catalogue"],
)
async def get_medication(med_id: str):
    m = await med_repo.find_by_id(med_id)
    if not m:
        raise HTTPException(404, "Médicament introuvable")
    return m


@router.post(
    "/catalog",
    summary="➕ Ajouter un médicament",
    tags=["Catalogue"],
    status_code=201,
)
async def add_medication(body: AddMedicationRequest):
    """
    Ajoute un médicament au catalogue MongoDB.
    ⚠️ Appeler POST /system/rag/rebuild après pour mettre à jour l'index RAG.
    """
    mid = await med_repo.add(body.model_dump())
    return {
        "message": "✅ Médicament ajouté",
        "mongodb_id": mid,
        "next_step": "Appeler POST /system/rag/rebuild pour mettre à jour l'index RAG",
    }


@router.put(
    "/catalog/{med_id}",
    summary="✏️ Modifier un médicament",
    tags=["Catalogue"],
)
async def update_medication(med_id: str, body: dict):
    ok = await med_repo.update(med_id, body)
    if not ok:
        raise HTTPException(404, "Médicament introuvable")
    return {"message": "✅ Médicament mis à jour",
            "next_step": "Appeler POST /system/rag/rebuild"}


@router.delete(
    "/catalog/{med_id}",
    summary="🗑️ Supprimer un médicament",
    tags=["Catalogue"],
)
async def delete_medication(med_id: str):
    ok = await med_repo.delete(med_id)
    if not ok:
        raise HTTPException(404, "Médicament introuvable")
    return {"message": "✅ Médicament supprimé"}


# ─────────────────────────────────────────────────────────────────────────────
# GROUPE 3 : RLHF
# ─────────────────────────────────────────────────────────────────────────────

@router.get(
    "/rlhf/dashboard",
    summary="📊 Dashboard RLHF",
    tags=["RLHF"],
)
async def rlhf_dashboard():
    """
    Tableau de bord qualité du modèle.
    Montre taux de validation, médicaments souvent rejetés/ajoutés.
    """
    return await rlhf_repo.dashboard()


@router.get(
    "/rlhf/scores",
    summary="🎯 Scores RLHF par médicament",
    tags=["RLHF"],
)
async def rlhf_scores():
    """
    Score de chaque médicament basé sur le feedback des pharmaciens.
    +1.0 = toujours validé | 0.0 = neutre | -1.0 = toujours rejeté
    """
    scores = await rlhf_repo.get_scores()
    return {
        "scores": scores,
        "total": len(scores),
        "interpretation": {
            "> 0.5":  "✅ Très bien accueilli par les pharmaciens",
            "0 à 0.5": "🟡 Généralement accepté",
            "< 0":    "⚠️ Souvent rejeté — à revoir",
        }
    }


# ─────────────────────────────────────────────────────────────────────────────
# GROUPE 4 : SYSTÈME
# ─────────────────────────────────────────────────────────────────────────────

@router.get(
    "/system/status",
    summary="🔧 État du moteur IA",
    tags=["Système"],
)
async def system_status(request: Request):
    """Retourne l'état du moteur : modèle chargé, nombre de médicaments indexés."""
    return request.app.state.engine.get_status()


@router.get(
    "/system/dashboard",
    summary="📈 Dashboard général",
    tags=["Système"],
)
async def general_dashboard(request: Request):
    """Statistiques complètes : recommandations + RLHF + catalogue + moteur."""
    return {
        "recommendations":    await rec_repo.stats(),
        "rlhf":               await rlhf_repo.dashboard(),
        "catalog":            {"total_medications": await med_repo.count()},
        "engine":             request.app.state.engine.get_status(),
        "generated_at":       datetime.utcnow().isoformat(),
    }


@router.post(
    "/system/rag/rebuild",
    summary="🔄 Reconstruire l'index RAG",
    tags=["Système"],
)
async def rebuild_rag(request: Request):
    """
    Recharge tous les médicaments depuis MongoDB
    et reconstruit les embeddings DistilBERT.
    À appeler après ajout/modification de médicaments.
    """
    meds = await med_repo.get_all()
    await request.app.state.engine.build_index(meds)
    return {"message": f"✅ Index RAG reconstruit avec {len(meds)} médicaments"}


@router.get(
    "/system/constitutional/rules",
    summary="⚖️ Règles Constitutional AI",
    tags=["Système"],
)
async def constitutional_rules():
    """Liste toutes les règles Constitutional AI actives."""
    return {
        "total": 9,
        "rules": [
            {"id": "R1", "name": "Ordonnance obligatoire",      "severity": "MEDIUM",   "action": "Avertissement pharmacien"},
            {"id": "R2", "name": "Allergies patient",           "severity": "HIGH",     "action": "Médicament retiré automatiquement"},
            {"id": "R3", "name": "Interactions dangereuses",    "severity": "HIGH",     "action": "Alerte pharmacien"},
            {"id": "R4", "name": "Grossesse",                   "severity": "HIGH",     "action": "Médicament retiré automatiquement"},
            {"id": "R5", "name": "Polypharmacie (max 3)",       "severity": "MEDIUM",   "action": "Liste réduite à 3"},
            {"id": "R6", "name": "Risque pédiatrique",          "severity": "HIGH",     "action": "Alerte posologie"},
            {"id": "R7", "name": "Insuffisance rénale",         "severity": "MEDIUM",   "action": "Alerte ajustement dose"},
            {"id": "R8", "name": "Urgences médicales",          "severity": "CRITICAL", "action": "Tout bloqué — redirection médecin"},
            {"id": "R9", "name": "Avertissement automédication","severity": "LOW",      "action": "Message systématique"},
        ]
    }
