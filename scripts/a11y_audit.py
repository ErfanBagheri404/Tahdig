"""#129 audit — two mechanical checks that can fail a build.

1. `minTouchTarget()` / under-48dp interactive controls. A visual 1px hairline
   with a click listener is a 1dp target: TalkBack and switch users get
   something they cannot hit. The fix is padding, not restyling — these scripts
   never propose changing the design, only the hit box.

2. List rows that are not ONE a11y stop. A row whose title, meta and state are
   separate nodes costs three swipes per dish; `mergeDescendants` makes it one
   stop with the state spoken in order.

Run: python scripts/a11y_audit.py   (exit 1 = a regression, for CI)
"""
import re
import pathlib
import sys

ROOT = pathlib.Path("app/src/main/java/com/erfanbagheri/tahdig")
MIN_DP = 48.0
problems = []

# ── 0. icon-only buttons with no label at all ─────────────────────────
for f in sorted(ROOT.rglob("*.kt")):
    src = f.read_text(encoding="utf-8")
    for m in re.finditer(r"\bIconButton\s*\(", src):
        start = m.end()
        depth, i = 1, start
        while i < len(src) and depth:
            if src[i] == "{":
                depth += 1
            elif src[i] == "}":
                depth -= 1
            i += 1
        body = src[start : i - 1]
        # A button is labelled if its subtree has text, or assigns a
        # contentDescription that is not literally null. The value may be a
        # conditional (`= if (x) "a" else "b"`), so counting assignments beats
        # matching `= "…"` — which is what gave a false positive on
        # FavoritesScreen's blocked/favorited toggle.
        n_text = len(re.findall(r"\bText\s*\(", body))
        n_desc = len(re.findall(r"contentDescription\s*=", body))
        n_null = len(re.findall(r"contentDescription\s*=\s*null", body))
        if n_text or n_desc > n_null:
            continue
        ln = src[: m.start()].count("\n") + 1
        problems.append(f"{f.relative_to(ROOT)}:{ln}  icon-only button with no label")

# ── 1. interactive controls pinned below the minimum target ─────────────
# Walk UP from the size() call to the modifier chain that owns it. A size is
# only a defect when the SAME chain also takes the tap: an icon glyph inside an
# IconButton that already carries minTouchTarget() is fine — the button's
# bounds are what a user hits, not the glyph's.
CHAIN_MARKER = re.compile(r"\.clickable\s*[({]|IconButton\(|Checkbox\(|Switch\(|combinedClickable\(")
FIXED = "minTouchTarget("
for f in sorted(ROOT.rglob("*.kt")):
    src = f.read_text(encoding="utf-8").replace("\r\n", "\n")
    lines = src.split("\n")
    for m in re.finditer(r"\.size\(\s*(\d+(?:\.\d+)?)\.dp\s*\)", src):
        if float(m.group(1)) >= MIN_DP:
            continue
        # The chain is the run of Modifier lines around this size. Take a
        # window that spans back to the start of the chain and forward past the
        # clickable, so a minTouchTarget() applied after the size still counts.
        pos = m.start()
        back = src.rfind("Modifier", max(0, pos - 600), pos)
        fwd = src.find("}", pos)
        chain = src[back : fwd if fwd > 0 else pos + 600]
        if not CHAIN_MARKER.search(chain):
            continue
        if FIXED in chain:
            continue
        ln = src[:pos].count("\n") + 1
        problems.append(
            f"{f.relative_to(ROOT)}:{ln}  {m.group(1)}dp on an interactive control"
        )

# ── 2. clickable rows with no merged semantics ────────────────────
# Per clickable, not per file: a row that wraps ONE Text is already a single
# stop and must not be flagged (a file-level test flagged HomeScreen, whose
# three rows are each a single label). A row with title + meta + state is
# three stops unless it merges. Our merge helpers: oneA11yStop (the wrapper),
# clearAndSetSemantics / mergeDescendants (hand-rolled), SwipeActionRow (the
# shared row that carries its own).
MERGED = re.compile(r"oneA11yStop\(|clearAndSetSemantics|mergeDescendants|SwipeActionRow")
for f in sorted(ROOT.rglob("*.kt")):
    src = f.read_text(encoding="utf-8").replace("\r\n", "\n")
    for m in re.finditer(r"\.clickable\s*[({]|combinedClickable\(", src):
        pos = m.start()
        # The row is the CALL that owns this modifier, not the 24 lines under
        # it: a fixed window borrowed the next card's Texts and flagged rows
        # that were already single stops. Walk back to the enclosing call and
        # forward to its match, trailing lambda included.
        #
        # Chain links do not count as the enclosing call: `.size(28.dp)` opens
        # a paren, so stopping there boxed a DiaryCard stepper and the NEXT
        # sibling together. A real enclosing call is a `(` whose previous
        # non-space char is not a `.` — that is `Box(`, `Row(`, `Surface(`.
        depth, i = 0, pos
        while i > 0:
            c = src[i]
            if c in ")]}":
                depth += 1
            elif c in "([{":
                if depth == 0 and c == "(":
                    k = i - 1
                    while k >= 0 and src[k].isspace():
                        k -= 1
                    if k < 0 or src[k] != ".":
                        break
                depth -= 1
            i -= 1
        if i <= 0:
            continue
        depth, j = 0, i
        while j < len(src):
            c = src[j]
            if c in "([{":
                depth += 1
            elif c in ")]}":
                depth -= 1
                if depth == 0:
                    # A trailing lambda belongs to the same call and holds the
                    # row's content: Row(modifier) { Text(); Text() }. Without
                    # this the scan stops at the arg list and sees no Texts,
                    # so every multi-node row passes.
                    k = j + 1
                    while k < len(src) and src[k].isspace():
                        k += 1
                    if k < len(src) and src[k] == "{":
                        j, depth = k, 0
                        continue
                    break
            j += 1
        window = src[i:j]
        if MERGED.search(window):
            continue
        nodes = len(re.findall(r"\bText\s*\(|\bIcon\s*\(", window))
        if nodes < 2:
            continue
        ln = src[:pos].count("\n") + 1
        problems.append(f"{f.relative_to(ROOT)}:{ln}  clickable row is not one a11y stop")

if problems:
    print(f"{len(problems)} accessibility problems\n")
    for p in problems:
        print("  " + p)
    sys.exit(1)

print("a11y audit clean")
