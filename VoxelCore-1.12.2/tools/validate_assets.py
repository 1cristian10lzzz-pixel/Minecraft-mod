#!/usr/bin/env python3
"""Lightweight validation for VoxelCore's Minecraft 1.12 resource pack."""
import json
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    Image = None

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/voxelcore"
BLOCKS = {"resonite_ore", "resonite_block", "resonite_lamp"}
ITEMS = {
    "resonite_crystal", "resonite_ingot", "resonite_pickaxe", "resonite_sword",
    "resonite_shovel", "resonite_axe", "resonite_hoe", "resonite_helmet",
    "resonite_chestplate", "resonite_leggings", "resonite_boots",
}
ERRORS = []


def fail(message):
    ERRORS.append(message)


def read_json(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        fail(f"Invalid JSON {path.relative_to(ROOT)}: {exc}")
        return None


for path in sorted(ASSETS.rglob("*.json")):
    data = read_json(path)
    if data is None:
        continue
    rel = path.relative_to(ASSETS).as_posix()
    if rel.startswith("blockstates/"):
        variants = data.get("variants", {})
        for variant in variants.values():
            model = variant.get("model")
            if model and model.startswith("voxelcore:"):
                model_path = ASSETS / "models" / "block" / (model.split(":", 1)[1] + ".json")
                if not model_path.exists():
                    fail(f"Missing blockstate model {model_path.relative_to(ROOT)}")
    if rel.startswith("models/"):
        for texture in data.get("textures", {}).values():
            if not isinstance(texture, str) or not texture.startswith("voxelcore:"):
                continue
            name = texture.split(":", 1)[1]
            candidates = [ASSETS / "textures" / (name + ".png")]
            if not any(candidate.exists() for candidate in candidates):
                fail(f"Missing texture for {path.relative_to(ROOT)}: {texture}")
    if rel.startswith("recipes/"):
        def inspect(value):
            if isinstance(value, dict):
                item = value.get("item")
                if isinstance(item, str) and item.startswith("voxelcore:"):
                    name = item.split(":", 1)[1]
                    if name not in BLOCKS and name not in ITEMS:
                        fail(f"Unregistered recipe item {item} in {path.relative_to(ROOT)}")
                for child in value.values():
                    inspect(child)
            elif isinstance(value, list):
                for child in value:
                    inspect(child)
        inspect(data)

for path in sorted((ASSETS / "textures").rglob("*.png")):
    if Image is not None:
        try:
            with Image.open(path) as image:
                image.verify()
            with Image.open(path) as image:
                width, height = image.size
            if path.parent.name == "armor" and (width, height) != (64, 32):
                fail(f"Armor texture must be 64x32: {path.relative_to(ROOT)} is {width}x{height}")
            if path.parent.name in ("items", "blocks") and (width, height) != (16, 16):
                fail(f"Item/block texture must be 16x16: {path.relative_to(ROOT)} is {width}x{height}")
        except Exception as exc:
            fail(f"Invalid PNG {path.relative_to(ROOT)}: {exc}")

for lang in (ASSETS / "lang").glob("*.lang"):
    seen = set()
    for line_number, line in enumerate(lang.read_text(encoding="utf-8").splitlines(), 1):
        if not line or line.lstrip().startswith("#") or "=" not in line:
            continue
        key = line.split("=", 1)[0]
        if key in seen:
            fail(f"Duplicate localization key {key} in {lang.name}:{line_number}")
        seen.add(key)

if ERRORS:
    print("VoxelCore asset validation failed:")
    for error in ERRORS:
        print(" - " + error)
    sys.exit(1)

print("VoxelCore asset validation passed.")
