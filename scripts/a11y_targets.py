"""#129 audit: touch targets under 48dp, and list rows that are not one a11y stop.

Two checks, both mechanical so they can run in CI:

1. `Modifier.size(N.dp)` / `width`/`height` below MIN_DP on an interactive node.
   A 1px divider with a click listener is a 1px target; TalkBack users get a
   target they cannot hit. Visual size stays as designed — the fix is padding.
2. Clickable rows in a LazyColumn that do not mergeDescendants, so TalkBack
   reads title / meta / state as three separate stops and a user pays three
   swipes per dish.
"""
import re, pathlib

ROOT = pathlib.Path("app/src/main/java/com/erfanbagheri/tahdig")
MIN_DP = 48.0

small = []
for f in sorted(ROOT.rglob("*.kt")):
    src = f.read_text(encoding="utf-8")
    for m in re.finditer(
        r"\.size\(\s*(\d+(?:\.\d+)?)\.dp\s*\)|\.width\(\s*(\d+(?:\.\d+)?)\.dp\s*\)|"
        r"\.height\(\s*(\d+(?:\.\d+)?)\.dp\s*\)",
        src,
    ):
        v = next(g for g in m.groups() if g)
        if float(v) < MIN_DP:
            ln = src[: m.start()].count("\n") + 1
            line = src.splitlines()[ln - 1].strip()
            small.append((str(f.relative_to(ROOT)), ln, v, line[:78]))

print(f"explicit sizes below {MIN_DP:.0f}dp: {len(small)}\n")
seen = set()
for path, ln, v, line in small:
    key = (path, ln)
    if key in seen:
        continue
    seen.add(key)
    print(f"  {path}:{ln}  {v}dp  {line}")

# Row merge check
rows = []
for f in sorted(ROOT.rglob("*.kt")):
    src = f.read_text(encoding="utf-8")
    if "LazyColumn" not in src:
        continue
    n_click = len(re.findall(r"\.clickable\s*[({]", src))
    n_merge = len(re.findall(r"mergeDescendants|clearAndSetSemantics", src))
    if n_click and not n_merge:
        rows.append((str(f.relative_to(ROOT)), n_click))
print(f"\nclickable-list screens with no merged semantics: {len(rows)}")
for path, n in rows:
    print(f"  {path}  ({n} clickables)")
