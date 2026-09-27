#!/usr/bin/env python
"""Bake assets/seed/equipment.json — per-dish equipment list (#100).

Two sources, unioned:
  1. category rules (a pizza dish needs an oven whether or not the tagline says so)
  2. keyword inference over description + ingredients, boundary-checked exactly
     like EquipmentInferrer.kt (label table mirrored below; the unit test
     guarantees every baked label exists in the Kotlin table).

Only non-empty entries are written, like images.json: the loader falls back to
runtime inference for anything missing.
"""
import glob
import json
import os

# Mirror of EquipmentInferrer.TABLE — single source of truth is the Kotlin table;
# EquipmentInferrerTest asserts every label baked here exists there.
TABLE = [
    "فر", "تابه", "قابلمه", "آبکش", "مخلوط‌کن", "همزن", "زودپز", "پلوپز",
    "سینی", "گریل", "کباب‌پز", "چرخ‌گوشت", "رنده", "مایکروویو", "دم‌کنی",
    "الک", "بخارپز", "توستر", "روغن‌گیر",
]

CATEGORY_EQUIPMENT = {
    1: ["قابلمه"],            # خورش
    2: ["قابلمه"],            # پلو و چلو
    3: ["تابه"],              # کباب (خانگی روی تابه)
    4: ["قابلمه"],            # آش
    5: ["قابلمه"],            # سوپ
    6: ["تابه"],              # کوکو و کتلت
    7: ["تابه"],              # املت
    8: ["قابلمه"],            # دلمه
    9: ["قابلمه"],            # آبگوشت و دیزی
    10: ["تابه"],             # ماهی
    11: ["قابلمه", "آبکش"],   # ماکارونی
    12: ["فر", "سینی"],       # پیتزا
    16: ["فر", "سینی"],       # نان
    18: ["تابه"],             # صبحانه
    19: ["قابلمه"],           # حلوا و دسر
    20: ["فر"],               # شیرینی
    25: ["تابه"],             # خوراک
}


def norm(s: str) -> str:
    out = []
    last_space = False
    for ch in s:
        if ch in "يىے":
            m = "ی"
        elif ch == "ك":
            m = "ک"
        elif ch == "ة":
            m = "ه"
        elif ch in "أإآ":
            m = "ا"
        elif ch == "ؤ":
            m = "و"
        elif ch == "ئ":
            m = "ی"
        elif ch == "ـ" or ch in "‌​‎‏﻿":
            continue
        elif "ً" <= ch <= "ۿ" and 0x064B <= ord(ch) <= 0x0652:
            continue
        else:
            m = ch
        if m.isspace():
            if not last_space:
                out.append(" ")
            last_space = True
        else:
            out.append(m)
            last_space = False
    return "".join(out).strip()


def is_fa(c):
    # Letters only: the Persian block also holds digits and «،» which must NOT
    # block a boundary (matches EquipmentInferrer's Character.isLetter rule).
    return c is not None and c.isalpha()


def contains_word(hay: str, needle: str) -> bool:
    if not needle:
        return False
    i = hay.find(needle)
    while i >= 0:
        before = hay[i - 1] if i > 0 else None
        end = i + len(needle)
        after = hay[end] if end < len(hay) else None
        if not is_fa(before) and not is_fa(after):
            return True
        i = hay.find(needle, i + 1)
    return False


def infer(texts):
    hay = norm(" ".join(texts))
    return [kw for kw in TABLE if contains_word(hay, norm(kw))]


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    foods_glob = os.path.join(root, "app/src/main/assets/seed/foods/*.json")
    out_path = os.path.join(root, "app/src/main/assets/seed/equipment.json")

    result = {}
    for path in sorted(glob.glob(foods_glob)):
        items = json.load(open(path, encoding="utf-8"))
        for item in items:
            labels = list(CATEGORY_EQUIPMENT.get(item.get("category_id"), []))
            for extra in infer([item.get("description", ""), item.get("ingredients", "")]):
                if extra not in labels:
                    labels.append(extra)
            if labels:
                unknown = [x for x in labels if x not in TABLE]
                assert not unknown, f"dish {item['id']}: labels not in table {unknown}"
                result[str(item["id"])] = labels

    with open(out_path, "w", encoding="utf-8") as fh:
        json.dump(dict(sorted(result.items(), key=lambda kv: int(kv[0]))),
                  fh, ensure_ascii=False, indent=0)
    print(f"{len(result)} dishes with equipment -> {out_path}")


if __name__ == "__main__":
    main()
