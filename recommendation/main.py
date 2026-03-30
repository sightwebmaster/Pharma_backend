"""
main.py
═════════════════════════════════════════════════════
Rôle : Point d'entrée du serveur FastAPI

Séquence de démarrage :
  1. Connexion MongoDB
  2. Chargement catalogue (JSON → MongoDB)
  3. Chargement DistilBERT
  4. Construction index RAG
  5. Serveur prêt ✅

Lancer :
  uvicorn main:app --reload --port 8085
"""

import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from db.mongodb import connect_db, close_db, load_catalog, MedicationRepo
from routers.router import router
from services.engine import RecommendationEngine

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Démarrage et arrêt propre du serveur."""

    # ── DÉMARRAGE ─────────────────────────────────────────────────────────────
    # 1. MongoDB
    await connect_db()

    # 2. Catalogue médicaments (JSON → MongoDB si vide)
    await load_catalog()

    # 3. DistilBERT
    engine = RecommendationEngine()
    engine.load_model()

    # 4. Index RAG (encode tous les médicaments)
    medications = await MedicationRepo().get_all()
    await engine.build_index(medications)

    # 5. Disponible dans toutes les routes
    app.state.engine = engine

    yield

    # ── ARRÊT ─────────────────────────────────────────────────────────────────
    await close_db()


app = FastAPI(
    title="💊 Système de Recommandation Médicamenteuse",
    description="""
## Architecture complète : DistilBERT + RAG + Constitutional AI + RLHF

### Processus
```
Patient écrit ses symptômes
        ↓
Constitutional AI  →  vérifie urgences (R8)
        ↓
DistilBERT         →  encode en vecteur 384-dim
        ↓
RAG                →  cherche dans 50 médicaments MongoDB
        ↓
Constitutional AI  →  filtre allergies, grossesse, interactions
        ↓
RLHF               →  ajuste scores selon historique pharmacien
        ↓
MongoDB            →  sauvegarde status=PENDING
        ↓
Pharmacien         →  VALIDATE / MODIFY / REJECT
        ↓
Patient            →  voit le résultat validé
```

### Endpoints principaux
| Endpoint | Description |
|----------|-------------|
| `POST /recommendations/analyze` | Analyser des symptômes |
| `GET /recommendations/pending` | Liste pour pharmacien |
| `PUT /recommendations/{id}/validate` | Valider (RLHF) |
| `GET /catalog` | Voir les 50 médicaments |
| `POST /catalog` | Ajouter un médicament |
| `GET /rlhf/dashboard` | Qualité du modèle |
| `GET /system/dashboard` | Stats complètes |
| `POST /system/rag/rebuild` | Mettre à jour index RAG |
    """,
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)


@app.get("/", tags=["Système"])
async def root():
    return {
        "service":  "Système de Recommandation Médicamenteuse",
        "version":  "1.0.0",
        "docs":     "http://localhost:8085/docs",
        "status":   "running",
    }


@app.get("/health", tags=["Système"])
async def health():
    return {"status": "UP"}
