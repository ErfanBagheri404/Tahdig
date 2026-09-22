"""Bake per-dish taste tags into assets/seed/flavor.json (#89).

Mirror of FlavorTagger.kt — keep the tables in sync. The issue wants tags baked
into the seed, not computed per launch; the Kotlin object stays as the tested
reference and the fallback for dishes added without a re-bake.

Run: python scripts/bake_flavor.py
"""
import json
import os
import re
import sys
from collections import Counter

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SEED = os.path.join(root, "app", "src", "main", "assets", "seed")

# --- tables mirrored from FlavorTagger.kt -----------------------------------
SOUR = ["لیمو", "سماق", "انار", "آبغوره", "غوره"]
SWEET = ["شیرینی", "شکر", "عسل", "گلاب", "مربا", "ژله", "بستنی",
         "حلوا", "فرنی", "کیک", "پودینگ", "دسر"]
BITTER = ["قهوه", "کاکائو", "نسکافه", "کنگر", "کاسنی"]
GREASY = ["کره", "سوخاری", "سرخ کردن", "سرخ شده", "سرخ کرده", "چرب"]
# ids 19 حلوا و دسر / 20 شیرینی / 21 بستنی و فالوده
SWEET_CATEGORY_IDS = {19, 20, 21}
# keyword heuristic mirrors SpiceProfile.level >= 2
HOT = ["فلفل", "تند", "چلی", "هریسا"]
MILD_SWEET = ["دسر", "شیرینی", "حلوا", "فرنی", "شله", "بستنی"]


def norm(s):
    # match PersianText.normalize closely enough for keyword tables:
    # fold Arabic yeh/kaf, drop ZWNJ; keywords themselves contain no ZWNJ variants.
    s = s.replace("ي", "ی").replace("ى", "ی").replace("ك", "ک")
    return s.replace("\u200c", "")


def has(text, words):
    return any(norm(w) in text for words_ in [words] for w in words_)


def spice_level(tags, ingredients, name):
    text = norm(f"{tags} {ingredients} {name}")
    if any(norm(w) in text for w in MILD_SWEET):
        return 0
    if any(norm(w) in text for w in HOT):
        if "فلفل قرمز" in text or "تند" in text or "هریسا" in text:
            return 3
        return 2
    return 1


def tag(ingredients, tags, name, category_id):
    text = norm(f"{ingredients} {tags} {name}")
    has_sour = has(text, SOUR)
    has_sweet = has(text, SWEET)
    is_sweet_dish = category_id in SWEET_CATEGORY_IDS or (has_sweet and not has_sour)
    if is_sweet_dish:
        out = ["SHIRIN"]
        if has(text, BITTER):
            out.append("TALKH")
        if has(text, GREASY):
            out.append("CHORB")
        return out
    out = []
    if has_sour:
        out.append("TURSH")
    # symmetric with the sweet gate: a spoon of شکر on a sour dish is not a sweet dish
    if has_sweet and not has_sour:
        out.append("SHIRIN")
    if has(text, BITTER):
        out.append("TALKH")
    if spice_level(tags, ingredients, name) >= 2:
        out.append("TOND")
    if has(text, GREASY):
        out.append("CHORB")
    return out


def main():
    cats = {}
    with open(os.path.join(SEED, "categories.json"), encoding="utf-8") as f:
        for c in json.load(f):
            cats[c["id"]] = c["name"]

    baked, per_axis = {}, Counter()
    dishes = 0
    for fn in sorted(os.listdir(SEED + "/foods")):
        if not fn.endswith(".json"):
            continue
        with open(os.path.join(SEED, "foods", fn), encoding="utf-8") as f:
            for item in json.load(f):
                dishes += 1
                labels = tag(item.get("ingredients", ""), item.get("tags", ""),
                             item.get("name", ""), item.get("category_id", 0))
                # ALL ids are written, empty list included: presence in the map
                # means "baked" — a missing id falls back to FlavorTagger at seed time.
                baked[str(item["id"])] = labels
                per_axis.update(labels)

    out = os.path.join(SEED, "flavor.json")
    with open(out, "w", encoding="utf-8") as f:
        json.dump(baked, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"dishes={dishes} tagged={len(baked)} axes={dict(per_axis)} -> {out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
