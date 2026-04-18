import csv
import logging
import os
from pathlib import Path
from typing import Optional

logger = logging.getLogger(__name__)

DEFAULT_DATASET_PATH = Path(__file__).resolve().parent.parent / "data" / "medical_enriched_dataset_large.csv"
DATASET_PATH = Path(os.getenv("DATASET_ENRICH_PATH", str(DEFAULT_DATASET_PATH)))
DATASET_HEADERS = ["ID", "symptoms", "disease", "recommended_medicines", "advice", "source"]


def _ensure_dataset_exists() -> None:
    DATASET_PATH.parent.mkdir(parents=True, exist_ok=True)
    if DATASET_PATH.exists():
        return

    with DATASET_PATH.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=DATASET_HEADERS)
        writer.writeheader()


def _next_id() -> int:
    _ensure_dataset_exists()
    last_id = 0
    with DATASET_PATH.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            try:
                last_id = max(last_id, int(str(row.get("ID", "0")).strip() or "0"))
            except ValueError:
                continue
    return last_id + 1


def _serialize_medications(medications: list[dict]) -> str:
    names = []
    for med in medications:
        if not isinstance(med, dict):
            continue
        name = str(med.get("name") or med.get("nom") or "").strip()
        if name:
            names.append(name)
    return " | ".join(dict.fromkeys(names))


def build_dataset_row(recommendation: dict, source_suffix: str) -> dict:
    advice_parts = [str(recommendation.get("dataset_advice") or "").strip()]
    note = str(recommendation.get("pharmacist_note") or "").strip()
    if note:
        advice_parts.append(f"Feedback pharmacien: {note}")

    medications = recommendation.get("recommended_medications") or []
    return {
        "ID": _next_id(),
        "symptoms": str(recommendation.get("symptoms") or "").strip(),
        "disease": str(recommendation.get("primary_disease") or "undetermined").strip(),
        "recommended_medicines": _serialize_medications(medications),
        "advice": " ".join(part for part in advice_parts if part).strip(),
        "source": f"recommendation-service:{source_suffix}",
    }


def append_dataset_row(recommendation: dict, source_suffix: str) -> dict:
    row = build_dataset_row(recommendation, source_suffix)
    _ensure_dataset_exists()
    with DATASET_PATH.open("a", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=DATASET_HEADERS)
        writer.writerow(row)

    logger.info("Dataset enriched with recommendation row id=%s source=%s", row["ID"], row["source"])
    return row


def enrich_recommendation(recommendation: dict) -> Optional[dict]:
    status = str(recommendation.get("status") or "").upper()
    if status not in {"VALIDATED", "MODIFIED"}:
        return None

    source_suffix = "rlhf_validate_auto_enrich" if status == "VALIDATED" else "rlhf_modify_auto_enrich"
    return append_dataset_row(recommendation, source_suffix)
