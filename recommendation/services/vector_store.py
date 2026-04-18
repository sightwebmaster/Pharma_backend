import hashlib
import json
import logging
from pathlib import Path
from typing import Optional

import numpy as np

logger = logging.getLogger(__name__)

try:
    import faiss  # type: ignore
except Exception:  # pragma: no cover
    faiss = None


class PersistentVectorStore:
    def __init__(self, base_dir: str | Path):
        self.base_dir = Path(base_dir)
        self.base_dir.mkdir(parents=True, exist_ok=True)
        self.index_path = self.base_dir / "medications.index.faiss"
        self.embeddings_path = self.base_dir / "medications.embeddings.npy"
        self.metadata_path = self.base_dir / "medications.metadata.json"
        self.fingerprint_path = self.base_dir / "medications.fingerprint.txt"
        self.index = None
        self.embeddings: Optional[np.ndarray] = None
        self.metadata: list[dict] = []
        self.backend = "faiss" if faiss is not None else "numpy"

    @staticmethod
    def build_fingerprint(items: list[dict]) -> str:
        digest = hashlib.sha256()
        for item in items:
            payload = {
                "id": item.get("id"),
                "name": item.get("name"),
                "dci": item.get("dci"),
                "category": item.get("category"),
                "disease": item.get("disease"),
                "indications": item.get("indications"),
                "description": item.get("description"),
            }
            digest.update(json.dumps(payload, ensure_ascii=False, sort_keys=True).encode("utf-8"))
        return digest.hexdigest()

    def load(self, expected_fingerprint: Optional[str] = None) -> bool:
        if not self.metadata_path.exists() or not self.fingerprint_path.exists():
            return False

        current_fingerprint = self.fingerprint_path.read_text(encoding="utf-8").strip()
        if expected_fingerprint and current_fingerprint != expected_fingerprint:
            return False

        self.metadata = json.loads(self.metadata_path.read_text(encoding="utf-8"))
        if faiss is not None and self.index_path.exists():
            self.index = faiss.read_index(str(self.index_path))
            self.backend = "faiss"
            logger.info("Persistent vector store loaded with FAISS (%s vectors)", len(self.metadata))
            return True

        if self.embeddings_path.exists():
            self.embeddings = np.load(self.embeddings_path)
            self.backend = "numpy"
            logger.warning("Persistent vector store loaded with NumPy fallback (%s vectors)", len(self.metadata))
            return True

        return False

    def save(self, embeddings: np.ndarray, metadata: list[dict], fingerprint: str) -> None:
        vectors = np.asarray(embeddings, dtype=np.float32)
        self.metadata = metadata

        self.metadata_path.write_text(
            json.dumps(metadata, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        self.fingerprint_path.write_text(fingerprint, encoding="utf-8")

        if faiss is not None:
            index = faiss.IndexFlatIP(vectors.shape[1])
            index.add(vectors)
            faiss.write_index(index, str(self.index_path))
            self.index = index
            self.backend = "faiss"
        else:
            np.save(self.embeddings_path, vectors)
            self.embeddings = vectors
            self.backend = "numpy"

        if faiss is None:
            logger.warning("FAISS unavailable, NumPy fallback persisted to disk")
        else:
            logger.info("Persistent FAISS index saved with %s vectors", len(metadata))

    def search(self, query_vector: np.ndarray, top_k: int) -> list[tuple[int, float]]:
        vector = np.asarray(query_vector, dtype=np.float32).reshape(1, -1)

        if faiss is not None and self.index is not None:
            scores, indices = self.index.search(vector, top_k)
            results = []
            for idx, score in zip(indices[0], scores[0]):
                if idx < 0:
                    continue
                results.append((int(idx), float(score)))
            return results

        if self.embeddings is None:
            raise RuntimeError("Vector store not initialized")

        scores = np.dot(self.embeddings, vector.squeeze(0))
        top_idx = np.argsort(scores)[::-1][:top_k]
        return [(int(idx), float(scores[idx])) for idx in top_idx]
