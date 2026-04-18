"""
db/mongodb.py
═════════════════════════════════════════════════════
Connexion MongoDB et Repositories

Collections :
  medications      → 40 médicaments (20 maladies × 2)
  recommendations  → résultats d'analyse (PENDING → VALIDATED/REJECTED/MODIFIED)
  rlhf_feedback    → feedback pharmaciens
"""

import os, json, logging
from pathlib import Path
from typing import Optional
from datetime import datetime
from motor.motor_asyncio import AsyncIOMotorClient
from bson import ObjectId

logger = logging.getLogger(__name__)

_client = None
_db     = None

# ─────────────────────────────────────────────────────────────
# CONNEXION
# ─────────────────────────────────────────────────────────────

async def connect_db():
    global _client, _db
    uri  = os.getenv("MONGODB_URI", "mongodb://localhost:27017")
    name = os.getenv("MONGODB_DB",  "pharmacare_reco")
    _client = AsyncIOMotorClient(uri)
    _db     = _client[name]
    logger.info(f"✅ MongoDB → {uri}/{name}")


async def close_db():
    if _client:
        _client.close()


def get_db():
    return _db


# ─────────────────────────────────────────────────────────────
# CHARGEMENT CATALOGUE
# ─────────────────────────────────────────────────────────────

async def load_catalog():
    """
    Charge medications.json → MongoDB au premier démarrage.
    Idempotent : ne fait rien si le catalogue existe déjà.

    Le catalogue contient 40 médicaments tirés du dataset :
    20 maladies × 2 médicaments chacune.
    """
    col   = get_db()["medications"]
    count = await col.count_documents({})

    if count > 0:
        logger.info(f"📚 Catalogue existant ({count} médicaments)")
        return

    path = Path(__file__).parent.parent / "data" / "medications.json"
    with open(path, encoding="utf-8") as f:
        meds = json.load(f)

    await col.insert_many(meds)

    # Index texte pour recherche
    await col.create_index([
        ("name",        "text"),
        ("dci",         "text"),
        ("disease",     "text"),
        ("indications", "text"),
        ("description", "text"),
    ])
    # Index par maladie
    await col.create_index("disease")

    logger.info(f"✅ {len(meds)} médicaments chargés (20 maladies × 2)")


# ─────────────────────────────────────────────────────────────
# MEDICATION REPOSITORY
# ─────────────────────────────────────────────────────────────

class MedicationRepo:
    @property
    def col(self):
        return get_db()["medications"]

    async def get_all(self) -> list[dict]:
        docs = []
        async for d in self.col.find({}):
            d["_id"] = str(d["_id"])
            docs.append(d)
        return docs

    async def find_by_id(self, med_id: str) -> Optional[dict]:
        d = await self.col.find_one({"id": med_id})
        if d:
            d["_id"] = str(d["_id"])
        return d

    async def find_by_disease(self, disease: str) -> list[dict]:
        docs = []
        async for d in self.col.find({"disease": {"$regex": disease, "$options": "i"}}):
            d["_id"] = str(d["_id"])
            docs.append(d)
        return docs

    async def search_text(self, query: str, limit: int = 10) -> list[dict]:
        docs = []
        async for d in self.col.find(
            {"$text": {"$search": query}},
            {"score": {"$meta": "textScore"}}
        ).sort([("score", {"$meta": "textScore"})]).limit(limit):
            d["_id"] = str(d["_id"])
            docs.append(d)
        return docs

    async def add(self, data: dict) -> str:
        r = await self.col.insert_one(data)
        return str(r.inserted_id)

    async def update(self, med_id: str, data: dict) -> bool:
        r = await self.col.update_one({"id": med_id}, {"$set": data})
        return r.modified_count > 0

    async def delete(self, med_id: str) -> bool:
        r = await self.col.delete_one({"id": med_id})
        return r.deleted_count > 0

    async def count(self) -> int:
        return await self.col.count_documents({})


# ─────────────────────────────────────────────────────────────
# RECOMMENDATION REPOSITORY
# ─────────────────────────────────────────────────────────────

class RecommendationRepo:
    """
    Structure d'un document recommandation :
    {
      _id                      : ObjectId
      patient_id               : string
      symptoms                 : string
      primary_disease          : string  ← nouveau (du dataset)
      dataset_advice           : string  ← conseil du dataset
      status                   : PENDING → VALIDATED / REJECTED / MODIFIED
      recommended_medications  : list
      constitutional_violations: list
      pharmacist_alerts        : list
      mandatory_warnings       : list
      redirect_to_doctor       : bool
      model                    : string
      latency_ms               : float
      created_at               : datetime
      validated_by             : string|null
      pharmacist_note          : string|null
      validated_at             : datetime|null
    }
    """

    @property
    def col(self):
        return get_db()["recommendations"]

    async def create(self, data: dict) -> str:
        data["status"]          = "PENDING"
        data["created_at"]      = datetime.utcnow()
        data["validated_by"]    = None
        data["pharmacist_note"] = None
        data["validated_at"]    = None
        data["dataset_enriched"] = False
        data["dataset_enriched_at"] = None
        r = await self.col.insert_one(data)
        return str(r.inserted_id)

    async def find_by_id(self, doc_id: str) -> Optional[dict]:
        try:
            d = await self.col.find_one({"_id": ObjectId(doc_id)})
            if d:
                d["id"] = str(d.pop("_id"))
            return d
        except:
            return None

    async def find_by_patient(self, patient_id: str, limit: int = 50) -> list[dict]:
        docs = []
        async for d in self.col.find(
            {"patient_id": patient_id}
        ).sort("created_at", -1).limit(limit):
            d["id"] = str(d.pop("_id"))
            docs.append(d)
        return docs

    async def find_pending(self) -> list[dict]:
        docs = []
        async for d in self.col.find({"status": "PENDING"}).sort("created_at", 1):
            d["id"] = str(d.pop("_id"))
            docs.append(d)
        return docs

    async def validate(self, doc_id: str, pharmacist_id: str,
                       action: str, note: Optional[str] = None,
                       modified_meds: Optional[list] = None) -> bool:
        status_map = {
            "VALIDATE": "VALIDATED",
            "REJECT":   "REJECTED",
            "MODIFY":   "MODIFIED",
        }
        update = {"$set": {
            "status":          status_map.get(action, action),
            "validated_by":    pharmacist_id,
            "pharmacist_note": note,
            "validated_at":    datetime.utcnow(),
            "feedback_action": action,
            "dataset_enriched": False,
            "dataset_enriched_at": None,
        }}
        if action == "MODIFY" and modified_meds:
            update["$set"]["recommended_medications"] = modified_meds

        r = await self.col.update_one({"_id": ObjectId(doc_id)}, update)
        return r.modified_count > 0

    async def mark_dataset_enriched(self, doc_id: str, dataset_row: Optional[dict] = None) -> bool:
        update = {
            "$set": {
                "dataset_enriched": True,
                "dataset_enriched_at": datetime.utcnow(),
            }
        }
        if dataset_row is not None:
            update["$set"]["dataset_row"] = dataset_row

        r = await self.col.update_one({"_id": ObjectId(doc_id)}, update)
        return r.modified_count > 0

    async def find_enrichable(self, recommendation_id: Optional[str] = None) -> list[dict]:
        query = {
            "status": {"$in": ["VALIDATED", "MODIFIED"]},
            "$or": [
                {"dataset_enriched": {"$exists": False}},
                {"dataset_enriched": False},
            ],
        }
        if recommendation_id:
            query["_id"] = ObjectId(recommendation_id)

        docs = []
        async for d in self.col.find(query).sort("validated_at", 1):
            d["id"] = str(d.pop("_id"))
            docs.append(d)
        return docs

    async def stats(self) -> dict:
        total     = await self.col.count_documents({})
        pending   = await self.col.count_documents({"status": "PENDING"})
        validated = await self.col.count_documents({"status": "VALIDATED"})
        rejected  = await self.col.count_documents({"status": "REJECTED"})
        modified  = await self.col.count_documents({"status": "MODIFIED"})
        enriched  = await self.col.count_documents({"dataset_enriched": True})
        rate      = f"{round(validated/total*100, 1)}%" if total else "0%"
        return {
            "total": total, "pending": pending,
            "validated": validated, "rejected": rejected,
            "modified": modified, "validation_rate": rate,
            "dataset_enriched": enriched,
        }


# ─────────────────────────────────────────────────────────────
# RLHF REPOSITORY
# ─────────────────────────────────────────────────────────────

class RLHFRepo:
    @property
    def col(self):
        return get_db()["rlhf_feedback"]

    async def save(self, data: dict) -> str:
        data["timestamp"] = datetime.utcnow()
        r = await self.col.insert_one(data)
        return str(r.inserted_id)

    async def get_scores(self) -> dict[str, float]:
        """Score RLHF par médicament ∈ [-1.0, +1.0]."""
        scores = {}
        async for d in self.col.find({"action": "VALIDATE"}):
            for m in d.get("original_medications", []):
                scores[m] = scores.get(m, 0.0) + 0.1

        async for d in self.col.find({"action": {"$in": ["REJECT", "MODIFY"]}}):
            for m in d.get("removed_by_pharmacist", []):
                scores[m] = scores.get(m, 0.0) - 0.2

        async for d in self.col.find({}):
            for m in d.get("added_by_pharmacist", []):
                scores[m] = scores.get(m, 0.0) + 0.3

        return {k: round(max(-1.0, min(1.0, v)), 4) for k, v in scores.items()}

    async def dashboard(self) -> dict:
        total     = await self.col.count_documents({})
        validated = await self.col.count_documents({"action": "VALIDATE"})
        rejected  = await self.col.count_documents({"action": "REJECT"})
        modified  = await self.col.count_documents({"action": "MODIFY"})
        vrate     = round(validated / total * 100, 1) if total else 0

        removed, added = {}, {}
        async for d in self.col.find({}):
            for m in d.get("removed_by_pharmacist", []):
                removed[m] = removed.get(m, 0) + 1
            for m in d.get("added_by_pharmacist", []):
                added[m] = added.get(m, 0) + 1

        quality = ("🟢 EXCELLENT" if vrate >= 80 else
                   "🟡 GOOD"      if vrate >= 60 else
                   "🟠 MEDIUM"    if vrate >= 40 else "🔴 POOR")

        return {
            "total_feedbacks": total,
            "validated":       validated,
            "rejected":        rejected,
            "modified":        modified,
            "validation_rate": f"{vrate}%",
            "most_removed":    sorted(removed.items(), key=lambda x: x[1], reverse=True)[:5],
            "most_added":      sorted(added.items(),   key=lambda x: x[1], reverse=True)[:5],
            "model_quality":   quality,
        }
