import json
import logging
import os
from typing import Any, Optional
from urllib import error, parse, request

logger = logging.getLogger(__name__)

USER_SERVICE_URL = os.getenv("USER_SERVICE_URL", "http://localhost:8083").rstrip("/")
MEDICATION_SERVICE_URL = os.getenv("MEDICATION_SERVICE_URL", "http://localhost:8088").rstrip("/")
INTERNAL_API_TOKEN = os.getenv("INTERNAL_API_TOKEN", "local-recommendation-token")
HTTP_TIMEOUT_SECONDS = float(os.getenv("BACKEND_CLIENT_TIMEOUT", "8"))


def _http_get_json(url: str, headers: Optional[dict[str, str]] = None) -> Any:
    req = request.Request(url, headers=headers or {}, method="GET")
    try:
        with request.urlopen(req, timeout=HTTP_TIMEOUT_SECONDS) as response:
            payload = response.read().decode("utf-8")
            return json.loads(payload) if payload else None
    except error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="ignore")
        logger.warning("Backend GET failed url=%s status=%s body=%s", url, exc.code, body[:300])
    except Exception as exc:
        logger.warning("Backend GET failed url=%s error=%s", url, exc)
    return None


def fetch_patient_context(patient_id: str) -> Optional[dict]:
    if not patient_id or patient_id == "anonymous":
        return None

    url = f"{USER_SERVICE_URL}/api/v1/patients/{parse.quote(patient_id)}/profile-for-reco"
    data = _http_get_json(url, {"X-User-Id": patient_id, "X-Internal-Token": INTERNAL_API_TOKEN})
    if not isinstance(data, dict):
        return None

    return {
        "user_id": data.get("userId", patient_id),
        "age": data.get("age"),
        "pregnant": bool(data.get("pregnant", False)),
        "allergies": data.get("allergies") or [],
        "conditions": data.get("conditions") or data.get("maladiesChroniques") or [],
        "maladies_chroniques": data.get("maladiesChroniques") or data.get("conditions") or [],
        "blood_type": data.get("groupeSanguin"),
    }


def _parse_price(value: Any) -> Optional[float]:
    if value is None:
        return None

    text = str(value).strip().replace(",", ".")
    digits = "".join(ch for ch in text if ch.isdigit() or ch == ".")
    if not digits:
        return None
    try:
        return round(float(digits), 3)
    except ValueError:
        return None


def normalize_medication(payload: dict) -> dict:
    return {
        "id": payload.get("id"),
        "name": payload.get("nom", ""),
        "dci": payload.get("principeActif", ""),
        "category": payload.get("source", "Medication Service"),
        "dosage": payload.get("dosage", ""),
        "frequency": payload.get("forme", ""),
        "form": payload.get("forme", ""),
        "disease": "",
        "indications": payload.get("symptomes") or [],
        "contraindications": payload.get("contreIndications") or [],
        "side_effects": payload.get("effetsSecondaires") or [],
        "allergenes": payload.get("allergenes") or [],
        "restrictions_age": payload.get("restrictionsAge") or [],
        "prescription_required": False,
        "price_tnd": _parse_price(payload.get("prix")) or 0.0,
        "advice": "",
        "description": payload.get("description", ""),
        "source": payload.get("source", "medication-service"),
    }


def fetch_medication_catalog() -> list[dict]:
    url = f"{MEDICATION_SERVICE_URL}/api/v1/medications/catalog"
    data = _http_get_json(url)
    if not isinstance(data, list):
        return []
    return [normalize_medication(item) for item in data if isinstance(item, dict)]
