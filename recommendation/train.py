"""
train.py
═══════════════════════════════════════════════════════════════
Fine-tuning DistilBERT sur dataset médical CSV

Format attendu du dataset.csv :
  symptomes,medicament
  "j'ai de la fièvre et des maux de tête",Paracétamol 500mg
  "j'ai mal au ventre et je vomis",Dompéridone 10mg
  ...

Résultat :
  → Dossier model_finetuned/ contenant le modèle entraîné
  → Ce modèle remplace distilbert-base dans engine.py

Lancer :
  python train.py
  python train.py --dataset mon_fichier.csv
  python train.py --epochs 10 --batch_size 32
"""

import os
import json
import argparse
import numpy as np
import pandas as pd
import torch
from torch.utils.data import Dataset, DataLoader
from transformers import (
    DistilBertTokenizer,
    DistilBertForSequenceClassification,
    get_linear_schedule_with_warmup,
)
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report
from tqdm import tqdm

# ─────────────────────────────────────────────────────────────
# CONFIGURATION
# ─────────────────────────────────────────────────────────────

def get_config():
    parser = argparse.ArgumentParser(description="Fine-tuning DistilBERT médical")
    parser.add_argument("--dataset",    default="dataset.csv",          help="Fichier CSV dataset")
    parser.add_argument("--output",     default="model_finetuned",       help="Dossier de sortie")
    parser.add_argument("--model",      default="distilbert-base-multilingual-cased")
    parser.add_argument("--epochs",     type=int,   default=5)
    parser.add_argument("--batch_size", type=int,   default=16)
    parser.add_argument("--max_len",    type=int,   default=128)
    parser.add_argument("--lr",         type=float, default=2e-5)
    parser.add_argument("--test_size",  type=float, default=0.2)
    parser.add_argument("--seed",       type=int,   default=42)
    return parser.parse_args()

# ─────────────────────────────────────────────────────────────
# DATASET PYTORCH
# ─────────────────────────────────────────────────────────────

class MedicalDataset(Dataset):
    """
    Convertit le CSV en format PyTorch.

    Chaque exemple :
      Input  : texte des symptômes tokenisé
      Output : ID du médicament (entier)

    Exemple :
      "j'ai de la fièvre" → tokens → [101, 1234, ...] → label=0 (Paracétamol)
    """

    def __init__(self, texts: list, labels: list, tokenizer, max_len: int):
        self.texts     = texts
        self.labels    = labels
        self.tokenizer = tokenizer
        self.max_len   = max_len

    def __len__(self):
        return len(self.texts)

    def __getitem__(self, idx):
        encoding = self.tokenizer(
            self.texts[idx],
            max_length=self.max_len,
            truncation=True,
            padding="max_length",
            return_tensors="pt",
        )
        return {
            "input_ids":      encoding["input_ids"].squeeze(0),
            "attention_mask": encoding["attention_mask"].squeeze(0),
            "labels":         torch.tensor(self.labels[idx], dtype=torch.long),
        }

# ─────────────────────────────────────────────────────────────
# CHARGEMENT ET PRÉPARATION DU DATASET
# ─────────────────────────────────────────────────────────────

def load_dataset(path: str, seed: int):
    """
    Lit le CSV et prépare les données.

    Colonnes attendues :
      - symptomes : texte des symptômes
      - medicament : nom du médicament cible

    Retourne :
      X_train, X_val, y_train, y_val, encoder, nb_classes
    """
    print(f"\n📂 Chargement du dataset : {path}")

    if not os.path.exists(path):
        raise FileNotFoundError(
            f"Dataset introuvable : {path}\n"
            f"Créer un fichier CSV avec les colonnes : symptomes, medicament"
        )

    df = pd.read_csv(path)

    # Vérifier les colonnes
    required = ["symptomes", "medicament"]
    missing  = [c for c in required if c not in df.columns]
    if missing:
        raise ValueError(
            f"Colonnes manquantes : {missing}\n"
            f"Colonnes trouvées : {list(df.columns)}"
        )

    # Nettoyer
    df = df.dropna(subset=required)
    df["symptomes"]  = df["symptomes"].astype(str).str.strip()
    df["medicament"] = df["medicament"].astype(str).str.strip()
    df = df[df["symptomes"].str.len() > 3]

    print(f"   → {len(df)} exemples valides")
    print(f"   → Distribution médicaments :")

    # Afficher distribution
    dist = df["medicament"].value_counts()
    for med, count in dist.head(10).items():
        bar = "█" * min(count // 10, 30)
        print(f"      {med[:30]:<30} {bar} ({count})")
    if len(dist) > 10:
        print(f"      ... {len(dist)-10} autres médicaments")

    # Encoder les médicaments → IDs numériques
    encoder    = LabelEncoder()
    df["label"] = encoder.fit_transform(df["medicament"])
    nb_classes = len(encoder.classes_)
    print(f"\n   → {nb_classes} médicaments distincts")

    # Split train / validation
    X_train, X_val, y_train, y_val = train_test_split(
        df["symptomes"].tolist(),
        df["label"].tolist(),
        test_size=0.2,
        random_state=seed,
        stratify=df["label"].tolist(),  # distribution équilibrée
    )
    print(f"   → Train : {len(X_train)} exemples")
    print(f"   → Val   : {len(X_val)} exemples")

    return X_train, X_val, y_train, y_val, encoder, nb_classes

# ─────────────────────────────────────────────────────────────
# ÉVALUATION
# ─────────────────────────────────────────────────────────────

def evaluate(model, loader, device):
    """
    Calcule la précision sur le dataset de validation.
    Retourne accuracy, toutes les prédictions et vrais labels.
    """
    model.eval()
    all_preds  = []
    all_labels = []

    with torch.no_grad():
        for batch in loader:
            input_ids      = batch["input_ids"].to(device)
            attention_mask = batch["attention_mask"].to(device)
            labels         = batch["labels"].to(device)

            outputs = model(
                input_ids=input_ids,
                attention_mask=attention_mask,
            )
            preds = outputs.logits.argmax(dim=1)
            all_preds.extend(preds.cpu().numpy())
            all_labels.extend(labels.cpu().numpy())

    accuracy = np.mean(np.array(all_preds) == np.array(all_labels)) * 100
    return accuracy, all_preds, all_labels

# ─────────────────────────────────────────────────────────────
# FINE-TUNING PRINCIPAL
# ─────────────────────────────────────────────────────────────

def train():
    cfg = get_config()

    # Reproductibilité
    torch.manual_seed(cfg.seed)
    np.random.seed(cfg.seed)

    print("=" * 60)
    print("  Fine-tuning DistilBERT — Système de Recommandation")
    print("=" * 60)
    print(f"  Modèle   : {cfg.model}")
    print(f"  Dataset  : {cfg.dataset}")
    print(f"  Epochs   : {cfg.epochs}")
    print(f"  Batch    : {cfg.batch_size}")
    print(f"  LR       : {cfg.lr}")
    print("=" * 60)

    # ── 1. Charger dataset ────────────────────────────────────
    X_train, X_val, y_train, y_val, encoder, nb_classes = load_dataset(
        cfg.dataset, cfg.seed
    )

    # ── 2. Charger tokenizer + modèle ────────────────────────
    print(f"\n⏳ Chargement DistilBERT ({cfg.model})...")
    tokenizer = DistilBertTokenizer.from_pretrained(cfg.model)

    # DistilBertForSequenceClassification = DistilBERT + couche
    # de classification sur mesure (nb_classes sorties)
    model = DistilBertForSequenceClassification.from_pretrained(
        cfg.model,
        num_labels=nb_classes,
    )
    print(f"   → Modèle chargé : {nb_classes} classes de sortie")

    # ── 3. DataLoaders ────────────────────────────────────────
    train_ds = MedicalDataset(X_train, y_train, tokenizer, cfg.max_len)
    val_ds   = MedicalDataset(X_val,   y_val,   tokenizer, cfg.max_len)

    train_loader = DataLoader(train_ds, batch_size=cfg.batch_size, shuffle=True)
    val_loader   = DataLoader(val_ds,   batch_size=cfg.batch_size)

    # ── 4. Optimizer + Scheduler ──────────────────────────────
    optimizer = torch.optim.AdamW(model.parameters(), lr=cfg.lr)
    total_steps = len(train_loader) * cfg.epochs
    scheduler = get_linear_schedule_with_warmup(
        optimizer,
        num_warmup_steps=total_steps // 10,  # 10% warmup
        num_training_steps=total_steps,
    )

    # ── 5. Device (GPU si disponible, sinon CPU) ───────────────
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"\n   → Device : {device}")
    if device.type == "cuda":
        print(f"   → GPU    : {torch.cuda.get_device_name(0)}")
    model.to(device)

    # ── 6. Boucle d'entraînement ──────────────────────────────
    print(f"\n🚀 Début du fine-tuning ({cfg.epochs} epochs)...\n")
    os.makedirs(cfg.output, exist_ok=True)
    best_val_acc = 0.0
    history      = []

    for epoch in range(cfg.epochs):
        print(f"Epoch {epoch+1}/{cfg.epochs}")
        print("-" * 40)

        # ── TRAIN ──
        model.train()
        total_loss = 0
        correct    = 0

        for batch in tqdm(train_loader, desc="  Training"):
            input_ids      = batch["input_ids"].to(device)
            attention_mask = batch["attention_mask"].to(device)
            labels         = batch["labels"].to(device)

            optimizer.zero_grad()

            outputs = model(
                input_ids=input_ids,
                attention_mask=attention_mask,
                labels=labels,
            )

            loss = outputs.loss
            loss.backward()                # calcul gradients
            torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            optimizer.step()               # mise à jour poids
            scheduler.step()               # ajustement learning rate

            total_loss += loss.item()
            preds       = outputs.logits.argmax(dim=1)
            correct    += (preds == labels).sum().item()

        train_loss = total_loss / len(train_loader)
        train_acc  = correct / len(X_train) * 100

        # ── VALIDATION ──
        val_acc, val_preds, val_labels = evaluate(model, val_loader, device)

        print(f"  Train  → Loss: {train_loss:.4f} | Accuracy: {train_acc:.1f}%")
        print(f"  Val    → Accuracy: {val_acc:.1f}%")

        history.append({
            "epoch":      epoch + 1,
            "train_loss": round(train_loss, 4),
            "train_acc":  round(train_acc, 2),
            "val_acc":    round(val_acc, 2),
        })

        # Sauvegarder le meilleur modèle
        if val_acc > best_val_acc:
            best_val_acc = val_acc
            model.save_pretrained(cfg.output)
            tokenizer.save_pretrained(cfg.output)
            print(f"  ✅ Meilleur modèle sauvegardé ({val_acc:.1f}%)")

        print()

    # ── 7. Rapport final ──────────────────────────────────────
    print("=" * 60)
    print(f"🎉 Fine-tuning terminé !")
    print(f"   Meilleure précision validation : {best_val_acc:.1f}%")
    print(f"   Modèle sauvegardé dans        : {cfg.output}/")
    print("=" * 60)

    # Rapport classification détaillé
    print("\n📊 Rapport détaillé sur validation :")
    val_acc_final, val_preds, val_labels = evaluate(model, val_loader, device)
    print(classification_report(
        val_labels, val_preds,
        target_names=encoder.classes_,
        zero_division=0,
    ))

    # ── 8. Sauvegarder métadonnées ────────────────────────────
    # Correspondance ID → nom médicament
    label_map = {str(i): name for i, name in enumerate(encoder.classes_)}
    with open(f"{cfg.output}/label_map.json", "w", encoding="utf-8") as f:
        json.dump(label_map, f, ensure_ascii=False, indent=2)

    # Historique entraînement
    with open(f"{cfg.output}/training_history.json", "w") as f:
        json.dump(history, f, indent=2)

    # Config du modèle
    meta = {
        "base_model":       cfg.model,
        "nb_classes":       nb_classes,
        "best_val_accuracy": round(best_val_acc, 2),
        "epochs":           cfg.epochs,
        "batch_size":       cfg.batch_size,
        "learning_rate":    cfg.lr,
        "max_length":       cfg.max_len,
        "medications":      list(encoder.classes_),
    }
    with open(f"{cfg.output}/meta.json", "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)

    print(f"\n✅ Fichiers sauvegardés dans {cfg.output}/")
    print(f"   - pytorch_model.bin   ← poids du modèle")
    print(f"   - config.json         ← configuration")
    print(f"   - tokenizer.json      ← tokenizer")
    print(f"   - label_map.json      ← ID → médicament")
    print(f"   - training_history.json")
    print(f"   - meta.json")
    print(f"\n▶  Lancer le serveur :")
    print(f"   uvicorn main:app --reload --port 8085")


if __name__ == "__main__":
    train()
