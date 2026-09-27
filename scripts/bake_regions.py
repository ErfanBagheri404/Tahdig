"""Bake regional cuisine overrides into assets/seed/regions.json (#90).

The seed's `cuisine` column carries coarse tags (IRANI / INTERNATIONAL); dish
names carry the regional markers the issue wants («کباب تبریزی» → آذربایجان).
This reads those markers once and writes a sparse {id: cuisine-key} map, merged
into FoodEntity.cuisine by SeedLoader at seed time — same sidecar ownership as
images/equipment/flavor, so the 27 dish files stay a plain diff.

Markers live ONLY here (not mirrored in Kotlin): Kotlin reads the baked output.

Run: python scripts/bake_regions.py
"""
import json
import os
import sys
from collections import Counter

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SEED = os.path.join(root, "app", "src", "main", "assets", "seed")

# name-marker -> regional cuisine key. Only IRANI/INTERNATIONAL dishes are
# retagged; a dish already carrying a specific tag (GILAKI, ITALIAN…) keeps it.
MARKERS = [
    ("AZERBAIJANI", ["تبریز", "آذربایجان", "اردبیل", "ارومیه"]),
    ("GILAKI", ["گیلان", "گیلک", "میرزاقاسمی"]),
    ("ISFAHANI", ["اصفهان"]),
    ("SHIRAZI", ["شیراز"]),
    ("YAZDI", ["یزد"]),
    ("KURDISH", ["کردستان", "سنندج", "کرمانشاه", "کردی"]),
    ("KHUZESTANI", ["خوزستان", "اهواز", "آبادان", "دزفول", "خرمشهر"]),
    ("MAZANDRANI", ["مازندران"]),
    ("KHORASANI", ["خراسان", "مشهد"]),
    ("LURISTAN", ["لرستان", "لری", "خرم‌آباد"]),
    ("BALUCHI", ["بلوچستان", "بلوچ"]),
]
RETAGGABLE = {"IRANI", "INTERNATIONAL"}


def main():
    overrides = {}
    per_key = Counter()
    dishes = 0
    for fn in sorted(os.listdir(os.path.join(SEED, "foods"))):
        if not fn.endswith(".json"):
            continue
        with open(os.path.join(SEED, "foods", fn), encoding="utf-8") as f:
            for item in json.load(f):
                dishes += 1
                name = item.get("name", "")
                cur = item.get("cuisine", "IRANI")
                if cur not in RETAGGABLE:
                    continue
                for key, words in MARKERS:
                    if any(w in name for w in words):
                        overrides[str(item["id"])] = key
                        per_key[key] += 1
                        break

    out = os.path.join(SEED, "regions.json")
    with open(out, "w", encoding="utf-8") as f:
        json.dump(overrides, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"dishes={dishes} retagged={len(overrides)} keys={dict(per_key)} -> {out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
