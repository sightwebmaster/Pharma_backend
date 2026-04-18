"""
routers/router.py
=================
Endpoints REST du recommendation-service.
"""

import logging
from datetime import datetime
from typing import Optional

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, Field

from db.mongodb import MedicationRepo, RecommendationRepo, RLHFRepo
from services.backend_clients import fetch_medication_catalog, fetch_patient_context
from services.dataset_enricher import enrich_recommendation

logger = logging.getLogger(__name__)

router = APIRouter()
rec_repo = RecommendationRepo()
med_repo = MedicationRepo()
rlhf_repo = RLHFRepo()


class AnalyzeRequest(BaseModel):
    symptoms: str = Field(..., min_length=3)
    patient_id: str = Field(default="anonymous")
    top_k: int = Field(default=5, ge=1, le=10)
    patient_profile: Optional[dict] = Field(default=None)


class ValidateRequest(BaseModel):
    pharmacist_id: str = Field(default="pharmacist-1")
    action: str = Field(..., pattern="^(VALIDATE|REJECT|MODIFY)$")
    pharmacist_note: Optional[str] = None
    modified_medications: Optional[list] = None


class AddMedicationRequest(BaseModel):
    id: str
    name: str
    dci: str
    category: str
    dosage: str
    frequency: str
    disease: str = Field(default="")
    indications: list[str]
    contraindications: list[str]
    side_effects: list[str]
    prescription_required: bool
    price_tnd: float
    advice: str = Field(default="")
    description: str


class DatasetEnrichRequest(BaseModel):
    recommendation_id: Optional[str] = None
    rebuild_index: bool = True


def _merge_patient_profiles(remote_profile: Optional[dict], payload_profile: Optional[dict]) -> dict:
    merged = {}
    for source in [remote_profile or {}, payload_profile or {}]:
        for key, value in source.items():
            if value is None:
                continue
            if isinstance(value, list):
                merged[key] = list(dict.fromkeys([str(item).strip() for item in value if str(item).strip()]))
            else:
                merged[key] = value

    if "maladies_chroniques" in merged and "conditions" not in merged:
        merged["conditions"] = merged["maladies_chroniques"]
    if "conditions" in merged and "maladies_chroniques" not in merged:
        merged["maladies_chroniques"] = merged["conditions"]
    return merged


async def _get_engine_catalog() -> tuple[list[dict], str]:
    meds = fetch_medication_catalog()
    if meds:
        return meds, "medication-service"

    meds = await med_repo.get_all()
    return meds, "mongodb-fallback"


async def _rebuild_engine_index(request: Request) -> tuple[int, str]:
    meds, source = await _get_engine_catalog()
    if not meds:
        raise HTTPException(503, "Medication catalog unavailable")

    await request.app.state.engine.build_index(meds, force_rebuild=True)
    request.app.state.catalog_source = source
    return len(meds), source


def _search_catalog_locally(medications: list[dict], query: str) -> list[dict]:
    q = query.lower().strip()
    results = []
    for med in medications:
        haystacks = [
            med.get("name", ""),
            med.get("dci", ""),
            med.get("description", ""),
            med.get("disease", ""),
            " ".join(med.get("indications", [])),
            " ".join(med.get("contraindications", [])),
        ]
        if any(q in str(value).lower() for value in haystacks if value):
            results.append(med)
    return results


async def _auto_enrich_recommendation(recommendation_id: str) -> Optional[dict]:
    recommendation = await rec_repo.find_by_id(recommendation_id)
    if not recommendation:
        return None
    if recommendation.get("dataset_enriched"):
        return recommendation.get("dataset_row")

    row = enrich_recommendation(recommendation)
    if not row:
        return None

    await rec_repo.mark_dataset_enriched(recommendation_id, row)
    return row


@router.post("/recommendations/analyze", summary="Analyze symptoms", tags=["Recommendations"], status_code=201)
async def analyze(body: AnalyzeRequest, request: Request):
    engine = request.app.state.engine
    rlhf_scores = await rlhf_repo.get_scores()
    remote_profile = fetch_patient_context(body.patient_id)
    patient_profile = _merge_patient_profiles(remote_profile, body.patient_profile)

    result = engine.analyze(
        symptoms=body.symptoms,
        patient_profile=patient_profile,
        rlhf_scores=rlhf_scores,
        top_k=body.top_k,
    )

    doc_id = await rec_repo.create({
        "patient_id": body.patient_id,
        "patient_profile": patient_profile,
        "profile_source": "user-service" if remote_profile else "request-body",
        **result,
    })
    return await rec_repo.find_by_id(doc_id)


@router.get("/recommendations/pending", summary="Pending recommendations", tags=["Recommendations"])
async def get_pending():
    return await rec_repo.find_pending()


@router.get("/recommendations/patient/{patient_id}", summary="Patient history", tags=["Recommendations"])
async def get_patient_history(patient_id: str):
    return await rec_repo.find_by_patient(patient_id)


@router.get("/recommendations/{rec_id}", summary="Get recommendation", tags=["Recommendations"])
async def get_recommendation(rec_id: str):
    doc = await rec_repo.find_by_id(rec_id)
    if not doc:
        raise HTTPException(404, "Recommendation not found")
    return doc


@router.put("/recommendations/{rec_id}/validate", summary="Validate / Modify / Reject", tags=["Recommendations"])
async def validate(rec_id: str, body: ValidateRequest):
    original = await rec_repo.find_by_id(rec_id)
    if not original:
        raise HTTPException(404, "Recommendation not found")

    ok = await rec_repo.validate(
        rec_id,
        body.pharmacist_id,
        body.action,
        body.pharmacist_note,
        body.modified_medications,
    )
    if not ok:
        raise HTTPException(400, "Already processed or not found")

    orig_meds = {m.get("name", "") for m in original.get("recommended_medications", [])}
    final_meds = set()

    if body.action == "VALIDATE":
        final_meds = orig_meds
    elif body.action == "MODIFY" and body.modified_medications:
        final_meds = {m.get("name", "") for m in body.modified_medications if isinstance(m, dict)}

    removed = list(orig_meds - final_meds)
    added = list(final_meds - orig_meds)
    agreement = len(orig_meds & final_meds) / len(orig_meds) if orig_meds else 1.0
    signal = (
        agreement if body.action == "VALIDATE"
        else agreement - 0.5 if body.action == "MODIFY"
        else -1.0
    )

    await rlhf_repo.save({
        "recommendation_id": rec_id,
        "patient_id": original.get("patient_id", ""),
        "pharmacist_id": body.pharmacist_id,
        "action": body.action,
        "symptoms": original.get("symptoms", ""),
        "primary_disease": original.get("primary_disease", ""),
        "original_medications": list(orig_meds),
        "final_medications": list(final_meds),
        "removed_by_pharmacist": removed,
        "added_by_pharmacist": added,
        "agreement_rate": round(agreement, 4),
        "rlhf_signal": round(signal, 4),
        "pharmacist_note": body.pharmacist_note,
    })

    enriched_row = None
    if body.action in {"VALIDATE", "MODIFY"}:
        enriched_row = await _auto_enrich_recommendation(rec_id)

    response = await rec_repo.find_by_id(rec_id)
    if enriched_row:
        response["dataset_row"] = enriched_row
    return response


@router.get("/catalog", summary="Full medication catalog", tags=["Catalog"])
async def list_catalog(
    category: Optional[str] = None,
    disease: Optional[str] = None,
    prescription: Optional[bool] = None,
):
    meds, source = await _get_engine_catalog()
    if category:
        meds = [m for m in meds if category.lower() in m.get("category", "").lower()]
    if disease:
        meds = [m for m in meds if disease.lower() in m.get("disease", "").lower()]
    if prescription is not None:
        meds = [m for m in meds if m.get("prescription_required") == prescription]
    return {"count": len(meds), "source": source, "medications": meds}


@router.get("/catalog/by-disease/{disease_name}", summary="Medications by disease", tags=["Catalog"])
async def get_by_disease(disease_name: str):
    meds, source = await _get_engine_catalog()
    results = [m for m in meds if disease_name.lower() in m.get("disease", "").lower()]
    if not results:
        raise HTTPException(404, f"No medications found for disease: {disease_name}")
    return {"disease": disease_name, "source": source, "count": len(results), "medications": results}


@router.get("/catalog/diseases", summary="List all diseases", tags=["Catalog"])
async def list_diseases():
    meds, source = await _get_engine_catalog()
    diseases = {}
    for med in meds:
        disease = med.get("disease", "Unknown") or "Unknown"
        diseases.setdefault(disease, []).append(med.get("name", ""))
    return {
        "source": source,
        "total_diseases": len(diseases),
        "total_medications": len(meds),
        "diseases": diseases,
    }


@router.get("/catalog/search", summary="Search medications", tags=["Catalog"])
async def search_catalog(q: str):
    meds, source = await _get_engine_catalog()
    results = _search_catalog_locally(meds, q)[:10]
    return {"count": len(results), "source": source, "results": results}


@router.get("/catalog/{med_id}", summary="Get medication detail", tags=["Catalog"])
async def get_medication(med_id: str):
    meds, source = await _get_engine_catalog()
    medication = next((m for m in meds if m.get("id") == med_id), None)
    if not medication:
        raise HTTPException(404, "Medication not found")
    medication["catalog_source"] = source
    return medication


@router.post("/catalog", summary="Add medication", tags=["Catalog"], status_code=201)
async def add_medication(body: AddMedicationRequest):
    mid = await med_repo.add(body.model_dump())
    return {
        "message": "Medication added to fallback Mongo catalog",
        "mongodb_id": mid,
        "next_step": "Call POST /system/dataset/enrich or /system/rag/rebuild to refresh the fallback RAG index",
    }


@router.put("/catalog/{med_id}", summary="Update medication", tags=["Catalog"])
async def update_medication(med_id: str, body: dict):
    ok = await med_repo.update(med_id, body)
    if not ok:
        raise HTTPException(404, "Medication not found")
    return {"message": "Updated fallback Mongo catalog entry"}


@router.delete("/catalog/{med_id}", summary="Delete medication", tags=["Catalog"])
async def delete_medication(med_id: str):
    ok = await med_repo.delete(med_id)
    if not ok:
        raise HTTPException(404, "Medication not found")
    return {"message": "Deleted fallback Mongo catalog entry"}


@router.get("/rlhf/dashboard", summary="RLHF Dashboard", tags=["RLHF"])
async def rlhf_dashboard():
    return await rlhf_repo.dashboard()


@router.get("/rlhf/scores", summary="RLHF scores per medication", tags=["RLHF"])
async def rlhf_scores():
    scores = await rlhf_repo.get_scores()
    return {
        "scores": scores,
        "total": len(scores),
        "interpretation": {
            "> 0.5": "Well validated by pharmacists",
            "0 to 0.5": "Generally accepted",
            "< 0": "Often rejected, review needed",
        },
    }


@router.get("/system/status", summary="Engine status", tags=["System"])
async def system_status(request: Request):
    status = request.app.state.engine.get_status()
    status["catalog_source"] = getattr(request.app.state, "catalog_source", "unknown")
    return status


@router.get("/system/dashboard", summary="General dashboard", tags=["System"])
async def general_dashboard(request: Request):
    meds, source = await _get_engine_catalog()
    return {
        "recommendations": await rec_repo.stats(),
        "rlhf": await rlhf_repo.dashboard(),
        "catalog": {"total_medications": len(meds), "source": source},
        "engine": request.app.state.engine.get_status(),
        "generated_at": datetime.utcnow().isoformat(),
    }


@router.post("/system/rag/rebuild", summary="Rebuild RAG index", tags=["System"])
async def rebuild_rag(request: Request):
    count, source = await _rebuild_engine_index(request)
    return {"message": f"RAG index rebuilt with {count} medications", "source": source}


@router.post("/system/dataset/enrich", summary="Enrich dataset and rebuild index", tags=["System"])
async def dataset_enrich(body: DatasetEnrichRequest, request: Request):
    try:
        recommendations = await rec_repo.find_enrichable(body.recommendation_id)
    except Exception as exc:
        logger.warning("Dataset enrichment lookup failed: %s", exc)
        raise HTTPException(400, "Invalid recommendation id") from exc

    enriched_rows = []
    for recommendation in recommendations:
        row = enrich_recommendation(recommendation)
        if not row:
            continue
        await rec_repo.mark_dataset_enriched(recommendation["id"], row)
        enriched_rows.append(row)

    rebuild_result = None
    if body.rebuild_index:
        count, source = await _rebuild_engine_index(request)
        rebuild_result = {"indexed_medications": count, "source": source}

    return {
        "processed_recommendations": len(recommendations),
        "enriched_rows": len(enriched_rows),
        "rows": enriched_rows,
        "rag_rebuild": rebuild_result,
    }


@router.get("/system/constitutional/rules", summary="Constitutional AI rules", tags=["System"])
async def constitutional_rules():
    return {
        "total": 9,
        "rules": [
            {"id": "R1", "name": "Prescription required", "severity": "MEDIUM", "action": "Pharmacist alert"},
            {"id": "R2", "name": "Patient allergies", "severity": "HIGH", "action": "Medication removed automatically"},
            {"id": "R3", "name": "Dangerous interactions", "severity": "HIGH", "action": "Pharmacist alert"},
            {"id": "R4", "name": "Pregnancy contraindication", "severity": "HIGH", "action": "Medication removed automatically"},
            {"id": "R5", "name": "Polypharmacy (max 3)", "severity": "MEDIUM", "action": "List reduced to 3"},
            {"id": "R6", "name": "Pediatric risk", "severity": "HIGH", "action": "Dosage alert"},
            {"id": "R7", "name": "Renal failure", "severity": "MEDIUM", "action": "Dose adjustment alert"},
            {"id": "R8", "name": "Medical emergency", "severity": "CRITICAL", "action": "Block all and redirect to emergency"},
            {"id": "R9", "name": "Self-medication warning", "severity": "LOW", "action": "Systematic message"},
        ],
    }


@router.get("/system/dataset/info", summary="Dataset information", tags=["System"])
async def dataset_info():
    return {
        "dataset_name": "medical_enriched_dataset_large.csv",
        "minimum_target_rows": 65000,
        "current_dataset_path": "recommendation/data/medical_enriched_dataset_large.csv",
        "columns": ["ID", "symptoms", "disease", "recommended_medicines", "advice", "source"],
        "enrichment_policy": {
            "VALIDATED": "Validated pharmacist decisions are appended automatically",
            "MODIFIED": "Modified pharmacist decisions are appended automatically",
        },
    }
