"""
train.py — PharmaCare v3
════════════════════════════════════════════════════════════════
Fine-tuning DistilBERT sur dataset médical enrichi (OpenFDA-backed)

Pipeline d'entraînement :
  CSV → Nettoyage → LabelEncoding → Stratified Split (80/20)
  → DistilBERT Tokenizer → Dataset → Fine-tuning → Évaluation → Save

Usage :
  python train.py
  python train.py --dataset data/medical_enriched_dataset.csv --epochs 15
  python train.py --model distilbert-base-uncased --batch_size 16 --lr 2e-5
"""

import os
import json
import argparse
import numpy as np
import pandas as pd
import torch
from torch.utils.data import Dataset, DataLoader
from transformers import (
    AutoModelForSequenceClassification,
    AutoTokenizer,
    get_linear_schedule_with_warmup,
)
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix
from collections import Counter
from tqdm import tqdm
import logging

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)


# ──────────────────────────────────────────────────────────────
# CONFIGURATION
# ──────────────────────────────────────────────────────────────

def get_config():
    p = argparse.ArgumentParser(description="Fine-tuning DistilBERT — PharmaCare v3")
    p.add_argument("--dataset",    default="data/medical_enriched_dataset_large.csv")
    p.add_argument("--output",     default="model_finetuned")
    p.add_argument("--model",      default="distilbert-base-multilingual-cased",
                   help="Recommended for French and multilingual symptoms")
    p.add_argument("--epochs",     type=int,   default=15)
    p.add_argument("--batch_size", type=int,   default=16)
    p.add_argument("--max_len",    type=int,   default=128)
    p.add_argument("--lr",         type=float, default=2e-5)
    p.add_argument("--test_size",  type=float, default=0.20,
                   help="Fraction used for validation (default 0.20 = 20%)")
    p.add_argument("--seed",       type=int,   default=42)
    p.add_argument("--sample",     type=int,   default=None,
                   help="Use only N rows (debug mode)")
    p.add_argument("--target",     default="disease",
                   choices=["disease", "medicine"])
    p.add_argument("--warmup_ratio", type=float, default=0.1,
                   help="Fraction of steps used for LR warmup")
    p.add_argument("--patience",   type=int,   default=5,
                   help="Early stopping patience (epochs without improvement)")
    return p.parse_args()


# ──────────────────────────────────────────────────────────────
# PYTORCH DATASET
# ──────────────────────────────────────────────────────────────

class MedicalDataset(Dataset):
    """
    Encapsule la tokenisation pour le DataLoader PyTorch.
    Chaque item retourne : input_ids, attention_mask, label.
    """
    def __init__(self, texts: list, labels: list, tokenizer, max_len: int):
        self.texts     = texts
        self.labels    = labels
        self.tokenizer = tokenizer
        self.max_len   = max_len

    def __len__(self):
        return len(self.texts)

    def __getitem__(self, idx):
        enc = self.tokenizer(
            self.texts[idx],
            max_length=self.max_len,
            truncation=True,
            padding="max_length",
            return_tensors="pt",
        )
        return {
            "input_ids":      enc["input_ids"].squeeze(0),
            "attention_mask": enc["attention_mask"].squeeze(0),
            "labels":         torch.tensor(self.labels[idx], dtype=torch.long),
        }


# ──────────────────────────────────────────────────────────────
# CHARGEMENT DATASET  (80 % train / 20 % val)
# ──────────────────────────────────────────────────────────────

def load_and_split(path: str, seed: int, target: str, sample: int,
                   test_size: float):
    """
    ┌─────────────────────────────────────────────┐
    │  PIPELINE DE PRÉPARATION DU DATASET          │
    │                                              │
    │  CSV Raw                                     │
    │    ↓ rename columns                          │
    │  Cleaned DataFrame                           │
    │    ↓ dropna + strip                          │
    │  Filtered DataFrame                          │
    │    ↓ LabelEncoder (disease → int)            │
    │  Encoded DataFrame                           │
    │    ↓ StratifiedShuffleSplit (80/20)          │
    │  Train Set (80%)  |  Validation Set (20%)    │
    │    ↓ MedicalDataset + DataLoader             │
    │  Ready for fine-tuning ✅                    │
    └─────────────────────────────────────────────┘

    Stratification garantit que chaque classe est représentée
    de façon proportionnelle dans train ET val.
    """
    print(f"\n📂 Loading dataset: {path}")

    if not os.path.exists(path):
        alt = "data/medical_question_answer_dataset_50000.csv"
        if os.path.exists(alt):
            print(f"   ⚠ Fallback to: {alt}")
            path = alt
        else:
            raise FileNotFoundError(f"Dataset not found: {path}")

    df = pd.read_csv(path, nrows=sample)
    print(f"   → {len(df)} rows loaded")
    print(f"   → Columns: {list(df.columns)}")

    # ── Normalise column names ─────────────────────────────────
    if "Symptoms/Question" in df.columns:
        df = df.rename(columns={
            "Symptoms/Question":     "symptoms",
            "Disease Prediction":    "disease",
            "Recommended Medicines": "medicine",
            "Advice":                "advice",
        })
    elif "recommended_medicines" in df.columns:
        df = df.rename(columns={"recommended_medicines": "medicine"})
    elif "symptomes" in df.columns:
        df = df.rename(columns={"symptomes": "symptoms", "maladie": "disease",
                                  "medicament": "medicine"})

    label_col = "disease" if target == "disease" else "medicine"
    print(f"   → Prediction target: {label_col}")

    # ── Clean ──────────────────────────────────────────────────
    df = df.dropna(subset=["symptoms", label_col])
    df["symptoms"]   = df["symptoms"].astype(str).str.strip()
    df[label_col]    = df[label_col].astype(str).str.strip()
    df               = df[df["symptoms"].str.len() > 5]

    # ── Remove duplicates only when dataset is very small ─────
    n_unique = df[["symptoms", label_col]].drop_duplicates().shape[0]
    if n_unique < 200:
        df = df.drop_duplicates(subset=["symptoms", label_col])

    nb_classes = df[label_col].nunique()
    print(f"\n   → {nb_classes} distinct classes")
    print(f"   → Class distribution (top 10):")
    for label, count in df[label_col].value_counts().head(10).items():
        bar = "█" * min(count, 25)
        print(f"      {label:<40} {bar} ({count})")

    # ── Encode labels ──────────────────────────────────────────
    encoder     = LabelEncoder()
    df["label"] = encoder.fit_transform(df[label_col])

    # ── Build disease → medication mapping ────────────────────
    metadata = {}
    if "disease" in df.columns and "medicine" in df.columns:
        for _, row in df.drop_duplicates(subset=["disease"]).iterrows():
            metadata[row["disease"]] = {
                "medicine": row.get("medicine", ""),
                "advice":   row.get("advice", ""),
            }

    # ── Stratified split 80 / 20 ──────────────────────────────
    #
    #  ┌──────────────────────────────────────────────┐
    #  │  Full Dataset  (N rows)                       │
    #  │  ┌──────────────────────┬───────────────┐    │
    #  │  │   TRAIN  80%         │  VALIDATION   │    │
    #  │  │   Used to update     │  20%          │    │
    #  │  │   model weights      │  Used ONLY    │    │
    #  │  │   (gradient descent) │  to measure   │    │
    #  │  │                      │  accuracy     │    │
    #  │  └──────────────────────┴───────────────┘    │
    #  │  Stratified: each class % is equal in both   │
    #  └──────────────────────────────────────────────┘
    #
    min_per_class = df["label"].value_counts().min()
    use_stratify  = min_per_class >= 2

    split_kwargs = dict(
        test_size    = test_size,
        random_state = seed,
    )
    if use_stratify:
        split_kwargs["stratify"] = df["label"].tolist()
    else:
        print("   ⚠ Some classes have <2 samples — skipping stratification")

    X_train, X_val, y_train, y_val = train_test_split(
        df["symptoms"].tolist(),
        df["label"].tolist(),
        **split_kwargs
    )

    print(f"\n   → Train : {len(X_train)} samples ({100-int(test_size*100)}%)")
    print(f"   → Val   : {len(X_val)} samples ({int(test_size*100)}%)")
    return X_train, X_val, y_train, y_val, encoder, nb_classes, metadata


# ──────────────────────────────────────────────────────────────
# EVALUATION
# ──────────────────────────────────────────────────────────────

def evaluate(model, loader, device):
    """Runs inference on the validation set and returns accuracy + predictions."""
    model.eval()
    all_preds, all_labels = [], []
    with torch.no_grad():
        for batch in loader:
            outputs = model(
                input_ids      = batch["input_ids"].to(device),
                attention_mask = batch["attention_mask"].to(device),
            )
            preds = outputs.logits.argmax(dim=1)
            all_preds.extend(preds.cpu().numpy())
            all_labels.extend(batch["labels"].numpy())

    acc = float(np.mean(np.array(all_preds) == np.array(all_labels))) * 100
    return acc, all_preds, all_labels


# ──────────────────────────────────────────────────────────────
# MAIN TRAINING LOOP
# ──────────────────────────────────────────────────────────────

def train():
    # ✅ Local numpy import prevents UnboundLocalError in any Python scope
    import numpy as _np
    _np.random.seed(42)

    cfg = get_config()
    torch.manual_seed(cfg.seed)

    print("=" * 65)
    print("  PharmaCare v3 — DistilBERT Fine-tuning")
    print("=" * 65)
    print(f"  Model       : {cfg.model}")
    print(f"  Dataset     : {cfg.dataset}")
    print(f"  Target      : {cfg.target}")
    print(f"  Epochs      : {cfg.epochs}")
    print(f"  Batch size  : {cfg.batch_size}")
    print(f"  LR          : {cfg.lr}")
    print(f"  Max tokens  : {cfg.max_len}")
    print(f"  Val split   : {int(cfg.test_size*100)}%")
    print(f"  Patience    : {cfg.patience} epochs")
    print("=" * 65)

    # ── 1. Load & split ───────────────────────────────────────
    X_train, X_val, y_train, y_val, encoder, nb_classes, metadata = load_and_split(
        cfg.dataset, cfg.seed, cfg.target, cfg.sample, cfg.test_size
    )

    # ── 2. Tokenizer + model ──────────────────────────────────
    print(f"\n⏳ Loading {cfg.model}...")
    tokenizer = AutoTokenizer.from_pretrained(cfg.model)
    model     = AutoModelForSequenceClassification.from_pretrained(
        cfg.model, num_labels=nb_classes,
        ignore_mismatched_sizes=True
    )
    print(f"   → Output classes: {nb_classes}")

    # ── 3. DataLoaders ────────────────────────────────────────
    train_ds = MedicalDataset(X_train, y_train, tokenizer, cfg.max_len)
    val_ds   = MedicalDataset(X_val,   y_val,   tokenizer, cfg.max_len)

    train_loader = DataLoader(
        train_ds,
        batch_size=min(cfg.batch_size, len(X_train)),
        shuffle=True,
        num_workers=0,
    )
    val_loader = DataLoader(
        val_ds,
        batch_size=min(cfg.batch_size, len(X_val)),
        num_workers=0,
    )

    # ── 4. Optimizer + scheduler ──────────────────────────────
    optimizer    = torch.optim.AdamW(model.parameters(), lr=cfg.lr, weight_decay=0.01)
    total_steps  = len(train_loader) * cfg.epochs
    warmup_steps = max(1, int(total_steps * cfg.warmup_ratio))
    scheduler    = get_linear_schedule_with_warmup(
        optimizer,
        num_warmup_steps  = warmup_steps,
        num_training_steps = total_steps,
    )
    print(f"   → Total steps: {total_steps}  |  Warmup: {warmup_steps}")

    # ── 5. Device ─────────────────────────────────────────────
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"   → Device: {device}")
    model.to(device)

    # ── 6. Training loop with early stopping ──────────────────
    print(f"\n🚀 Starting fine-tuning ({cfg.epochs} epochs, patience={cfg.patience})...\n")
    os.makedirs(cfg.output, exist_ok=True)

    best_val_acc    = 0.0
    history         = []
    patience_count  = 0

    for epoch in range(cfg.epochs):
        print(f"Epoch {epoch+1}/{cfg.epochs}")
        print("-" * 50)

        model.train()
        total_loss, correct = 0.0, 0

        for batch in tqdm(train_loader, desc="  Training"):
            input_ids      = batch["input_ids"].to(device)
            attention_mask = batch["attention_mask"].to(device)
            labels         = batch["labels"].to(device)

            optimizer.zero_grad()
            outputs = model(input_ids=input_ids, attention_mask=attention_mask, labels=labels)
            outputs.loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            optimizer.step()
            scheduler.step()

            total_loss += outputs.loss.item()
            correct    += (outputs.logits.argmax(1) == labels).sum().item()

        train_loss = total_loss / len(train_loader)
        train_acc  = correct   / len(X_train) * 100
        val_acc, val_preds, val_true = evaluate(model, val_loader, device)

        print(f"  Train  → Loss: {train_loss:.4f} | Acc: {train_acc:.1f}%")
        print(f"  Val    → Acc: {val_acc:.1f}%")

        history.append({
            "epoch":      epoch + 1,
            "train_loss": round(train_loss, 4),
            "train_acc":  round(train_acc,  2),
            "val_acc":    round(val_acc,    2),
        })

        if val_acc > best_val_acc:
            best_val_acc   = val_acc
            patience_count = 0
            model.save_pretrained(cfg.output)
            tokenizer.save_pretrained(cfg.output)
            print(f"  ✅ Best model saved ({val_acc:.1f}%)")
        else:
            patience_count += 1
            print(f"  ⏳ No improvement ({patience_count}/{cfg.patience})")
            if patience_count >= cfg.patience:
                print(f"\n  🛑 Early stopping triggered at epoch {epoch+1}")
                break
        print()

    # ── 7. Final report ───────────────────────────────────────
    print("=" * 65)
    print(f"🎉 Training complete!")
    print(f"   Best validation accuracy: {best_val_acc:.1f}%")
    print("=" * 65)

    # Reload best model for final evaluation
    best_model = AutoModelForSequenceClassification.from_pretrained(
        cfg.output, num_labels=nb_classes, ignore_mismatched_sizes=True
    ).to(device)
    _, final_preds, final_true = evaluate(best_model, val_loader, device)

    print("\n📊 Detailed classification report:")
    print(classification_report(
        final_true, final_preds,
        target_names=encoder.classes_,
        zero_division=0,
    ))

    # ── 8. Save metadata ──────────────────────────────────────
    label_map = {str(i): name for i, name in enumerate(encoder.classes_)}
    with open(f"{cfg.output}/label_map.json", "w", encoding="utf-8") as f:
        json.dump(label_map, f, ensure_ascii=False, indent=2)

    with open(f"{cfg.output}/disease_to_meds.json", "w", encoding="utf-8") as f:
        json.dump(metadata, f, ensure_ascii=False, indent=2)

    with open(f"{cfg.output}/training_history.json", "w") as f:
        json.dump(history, f, indent=2)

    meta = {
        "base_model":          cfg.model,
        "language_profile":    "multilingual-fr-first",
        "nb_classes":          nb_classes,
        "classes":             list(encoder.classes_),
        "target":              cfg.target,
        "best_val_accuracy":   round(best_val_acc, 2),
        "epochs_trained":      len(history),
        "early_stopped":       patience_count >= cfg.patience,
        "batch_size":          cfg.batch_size,
        "learning_rate":       cfg.lr,
        "max_length":          cfg.max_len,
        "val_split":           cfg.test_size,
        "dataset":             cfg.dataset,
        "train_samples":       len(X_train),
        "val_samples":         len(X_val),
    }
    with open(f"{cfg.output}/meta.json", "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)

    print(f"\n✅ Saved to {cfg.output}/")
    print(f"   label_map.json       ← int ID → disease name")
    print(f"   disease_to_meds.json ← disease → medications mapping")
    print(f"   meta.json            ← training configuration & results")
    print(f"   training_history.json")
    print(f"\n▶  Start service:")
    print(f"   uvicorn main:app --reload --port 8095")


if __name__ == "__main__":
    train()
