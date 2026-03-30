"""
db/mongodb.py
═════════════════════════════════════════════════════
Rôle : Tout ce qui touche à MongoDB

Contient :
  - Connexion / Déconnexion
  - Chargement catalogue médicaments
  - MedicationRepo     → lire/ajouter/modifier les médicaments
  - RecommendationRepo → sauvegarder/lire les recommandations
  - RLHFRepo           → sauvegarder feedback + calculer scores
"""

import os, json, logging
from pathlib import Path
from typing import Optional
from datetime import datetime
from motor.motor_asyncio import AsyncIOMotorClient
from bson import ObjectId

logger = logging.getLogger(__name__)

# ── Variables globales connexion ──────────────────────────────────────────────
_client = None
_db     = None

# ─────────────────────────────────────────────────────────────────────────────
# CONNEXION
# ─────────────────────────────────────────────────────────────────────────────

async def connect_db():
    """Ouvre la connexion MongoDB au démarrage du serveur."""
    global _client, _db
    uri  = os.getenv("MONGODB_URI", "mongodb://localhost:27017")
    name = os.getenv("MONGODB_DB",  "pharmacare")
    _client = AsyncIOMotorClient(uri)
    _db     = _client[name]
    logger.info(f"✅ MongoDB connecté → {uri}/{name}")

async def close_db():
    """Ferme la connexion proprement à l'arrêt du serveur."""
    if _client:
        _client.close()

def get_db():
    """Retourne l'instance de la base de données."""
    return _db

# ─────────────────────────────────────────────────────────────────────────────
# CHARGEMENT CATALOGUE
# ─────────────────────────────────────────────────────────────────────────────

async def load_catalog():
    """
    Charge les médicaments depuis medications.json vers MongoDB.
    Appelé une seule fois au démarrage.
    Si le catalogue existe déjà → ne fait rien (idempotent).
    """
    col   = get_db()["medications"]
    count = await col.count_documents({})

    if count > 0:
        logger.info(f"📚 Catalogue déjà présent ({count} médicaments)")
        return

    path = Path(__file__).parent.parent / "data" / "medications.json"
    with open(path, encoding="utf-8") as f:
        meds = json.load(f)

    await col.insert_many(meds)

    # Index texte pour recherche rapide
    await col.create_index([
        ("name",        "text"),
        ("dci",         "text"),
        ("indications", "text"),
        ("description", "text"),
    ])

    logger.info(f"✅ {len(meds)} médicaments chargés dans MongoDB")

# ─────────────────────────────────────────────────────────────────────────────
# REPOSITORY MÉDICAMENTS
# ─────────────────────────────────────────────────────────────────────────────

class MedicationRepo:
    """
    Accès aux médicaments dans MongoDB.
    Utilisé par le moteur RAG pour charger le catalogue.
    """

    @property
    def col(self):
        return get_db()["medications"]

    async def get_all(self) -> list[dict]:
        """Retourne tous les médicaments → utilisé pour construire l'index RAG."""
        docs = []
        async for d in self.col.find({}):
            d["_id"] = str(d["_id"])
            docs.append(d)
        return docs

    async def find_by_id(self, med_id: str) -> Optional[dict]:
        """Cherche un médicament par son ID (ex: med001)."""
        d = await self.col.find_one({"id": med_id})
        if d:
            d["_id"] = str(d["_id"])
        return d

    async def search_text(self, query: str, limit: int = 10) -> list[dict]:
        """Recherche textuelle MongoDB (nom, indications, description)."""
        docs = []
        async for d in self.col.find(
            {"$text": {"$search": query}},
            {"score": {"$meta": "textScore"}}
        ).sort([("score", {"$meta": "textScore"})]).limit(limit):
            d["_id"] = str(d["_id"])
            docs.append(d)
        return docs

    async def add(self, data: dict) -> str:
        """Ajoute un nouveau médicament au catalogue."""
        r = await self.col.insert_one(data)
        return str(r.inserted_id)

    async def update(self, med_id: str, data: dict) -> bool:
        """Modifie un médicament existant."""
        r = await self.col.update_one({"id": med_id}, {"$set": data})
        return r.modified_count > 0

    async def delete(self, med_id: str) -> bool:
        """Supprime un médicament du catalogue."""
        r = await self.col.delete_one({"id": med_id})
        return r.deleted_count > 0

    async def count(self) -> int:
        return await self.col.count_documents({})

# ─────────────────────────────────────────────────────────────────────────────
# REPOSITORY RECOMMANDATIONS
# ─────────────────────────────────────────────────────────────────────────────

class RecommendationRepo:
    """
    Sauvegarde et lecture des recommandations.

    Structure d'un document recommandation :
    {
      _id                      : ObjectId (généré par MongoDB)
      patient_id               : string
      symptoms                 : string (texte original)
      language                 : "fr" ou "ar"
      status                   : PENDING → VALIDATED / REJECTED / MODIFIED
      recommended_medications  : liste des médicaments avec scores
      constitutional_violations: violations détectées
      pharmacist_alerts        : alertes pour le pharmacien
      mandatory_warnings       : avertissements pour le patient
      redirect_to_doctor       : bool (urgence ?)
      model                    : nom du modèle utilisé
      latency_ms               : temps de traitement
      created_at               : datetime
      validated_by             : id pharmacien (null si PENDING)
      pharmacist_note          : note du pharmacien
      validated_at             : datetime de validation
    }
    """

    @property
    def col(self):
        return get_db()["recommendations"]

    async def create(self, data: dict) -> str:
        """Sauvegarde une nouvelle recommandation avec status PENDING."""
        data["status"]          = "PENDING"
        data["created_at"]      = datetime.utcnow()
        data["validated_by"]    = None
        data["pharmacist_note"] = None
        data["validated_at"]    = None
        r = await self.col.insert_one(data)
        return str(r.inserted_id)

    async def find_by_id(self, doc_id: str) -> Optional[dict]:
        """Cherche une recommandation par son ID MongoDB."""
        try:
            d = await self.col.find_one({"_id": ObjectId(doc_id)})
            if d:
                d["id"] = str(d.pop("_id"))
            return d
        except:
            return None

    async def find_by_patient(self, patient_id: str, limit: int = 50) -> list[dict]:
        """Retourne l'historique des recommandations d'un patient."""
        docs = []
        async for d in self.col.find({"patient_id": patient_id}).sort("created_at", -1).limit(limit):
            d["id"] = str(d.pop("_id"))
            docs.append(d)
        return docs

    async def find_pending(self) -> list[dict]:
        """Retourne toutes les recommandations en attente de validation."""
        docs = []
        async for d in self.col.find({"status": "PENDING"}).sort("created_at", 1):
            d["id"] = str(d.pop("_id"))
            docs.append(d)
        return docs

    async def validate(self, doc_id: str, pharmacist_id: str,
                       action: str, note: Optional[str] = None,
                       modified_meds: Optional[list] = None) -> bool:
        """Met à jour le statut après validation du pharmacien."""
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
        }}
        if action == "MODIFY" and modified_meds:
            update["$set"]["recommended_medications"] = modified_meds

        r = await self.col.update_one({"_id": ObjectId(doc_id)}, update)
        return r.modified_count > 0

    async def stats(self) -> dict:
        """Statistiques générales des recommandations."""
        total     = await self.col.count_documents({})
        pending   = await self.col.count_documents({"status": "PENDING"})
        validated = await self.col.count_documents({"status": "VALIDATED"})
        rejected  = await self.col.count_documents({"status": "REJECTED"})
        modified  = await self.col.count_documents({"status": "MODIFIED"})
        rate      = f"{round(validated/total*100, 1)}%" if total else "0%"
        return {
            "total": total, "pending": pending,
            "validated": validated, "rejected": rejected,
            "modified": modified, "validation_rate": rate,
        }

# ─────────────────────────────────────────────────────────────────────────────
# REPOSITORY RLHF
# ─────────────────────────────────────────────────────────────────────────────

class RLHFRepo:
    """
    Sauvegarde le feedback des pharmaciens et calcule les scores RLHF.

    Structure d'un document feedback :
    {
      recommendation_id     : ID de la recommandation concernée
      pharmacist_id         : ID du pharmacien
      action                : VALIDATE / REJECT / MODIFY
      original_medications  : médicaments proposés par l'IA
      final_medications     : médicaments retenus par le pharmacien
      removed_by_pharmacist : médicaments retirés
      added_by_pharmacist   : médicaments ajoutés
      agreement_rate        : % de médicaments conservés (0 → 1)
      rlhf_signal           : signal normalisé (-1 → +1)
      timestamp             : datetime
    }
    """

    @property
    def col(self):
        return get_db()["rlhf_feedback"]

    async def save(self, data: dict) -> str:
        """Enregistre un feedback pharmacien."""
        data["timestamp"] = datetime.utcnow()
        r = await self.col.insert_one(data)
        return str(r.inserted_id)

    async def get_scores(self) -> dict[str, float]:
        """
        Calcule le score RLHF de chaque médicament.
        Score ∈ [-1.0, +1.0]
          +1.0 = toujours validé
           0.0 = neutre / pas de données
          -1.0 = toujours rejeté
        """
        scores = {}

        # Médicaments souvent validés → bonus
        async for d in self.col.find({"action": "VALIDATE"}):
            for m in d.get("original_medications", []):
                scores[m] = scores.get(m, 0.0) + 0.1

        # Médicaments retirés par le pharmacien → malus
        async for d in self.col.find({"action": {"$in": ["REJECT", "MODIFY"]}}):
            for m in d.get("removed_by_pharmacist", []):
                scores[m] = scores.get(m, 0.0) - 0.2

        # Médicaments ajoutés par le pharmacien → fort bonus
        async for d in self.col.find({}):
            for m in d.get("added_by_pharmacist", []):
                scores[m] = scores.get(m, 0.0) + 0.3

        # Normaliser dans [-1, +1]
        return {k: round(max(-1.0, min(1.0, v)), 4) for k, v in scores.items()}

    async def dashboard(self) -> dict:
        """Dashboard RLHF : métriques qualité du modèle."""
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
                   "🟡 BON"       if vrate >= 60 else
                   "🟠 MOYEN"     if vrate >= 40 else "🔴 FAIBLE")

        return {
            "total_feedbacks":  total,
            "validated":        validated,
            "rejected":         rejected,
            "modified":         modified,
            "validation_rate":  f"{vrate}%",
            "most_removed":     sorted(removed.items(), key=lambda x: x[1], reverse=True)[:5],
            "most_added":       sorted(added.items(),   key=lambda x: x[1], reverse=True)[:5],
            "model_quality":    quality,
        }
