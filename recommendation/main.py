"""main.py — PharmaCare v3 — port 8085"""
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from db.mongodb import connect_db, close_db, load_catalog, MedicationRepo
from routers.router import router
from services.backend_clients import fetch_medication_catalog
from services.engine import RecommendationEngine

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")


async def _load_engine_catalog() -> tuple[list[dict], str]:
    medications = fetch_medication_catalog()
    if medications:
        logging.info("Loaded %s medications from medication-service", len(medications))
        return medications, "medication-service"

    await load_catalog()
    medications = await MedicationRepo().get_all()
    logging.warning("Falling back to MongoDB catalog with %s medications", len(medications))
    return medications, "mongodb-fallback"

@asynccontextmanager
async def lifespan(app: FastAPI):
    await connect_db()
    engine = RecommendationEngine()
    engine.load_model()
    medications, source = await _load_engine_catalog()
    await engine.build_index(medications)
    app.state.engine = engine
    app.state.catalog_source = source
    yield
    await close_db()

app = FastAPI(
    title="💊 PharmaCare — Recommendation Service v3",
    description="DistilBERT + RAG + OpenFDA + Constitutional AI + RLHF",
    version="3.0.0",
    lifespan=lifespan,
)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
app.include_router(router)

@app.get("/", tags=["System"])
async def root():
    return {"service": "PharmaCare Recommendation Service", "version": "3.0.0",
            "docs": "http://localhost:8095/docs", "status": "running"}

@app.get("/health", tags=["System"])
async def health():
    return {"status": "UP"}
