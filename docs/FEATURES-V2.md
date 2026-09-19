# Tahdig — Feature Roadmap (v2)

Research-backed feature list to make Tahdig a full-stack app. Each item becomes one issue + one PR.

## Core UX
1. **Shopping list (لیست خرید)** — auto-generate from a suggested dish's ingredients; checkable items, persisted in Room.
2. **Ingredient-based search (جستجو با مواد)** — "what can I cook with X, Y?" filters dishes whose ingredients match.
3. **Serving size scaling (تعداد نفرات)** — pick 2/4/6 people, scale ingredient quantities.
4. **Cooking timer (تایمر پخت)** — per-dish countdown timer with notification.
5. **Step-by-step cooking mode (حالت پخت)** — full-screen step navigation with keep-screen-on.

## Personalization
6. **Dietary filters (فیلتر رژیمی)** — vegetarian/vegan/no-gluten/low-cal from `tags`.
7. **Spice/preference profile (پروفایل سلیقه)** — dislike spicy/sweet; re-ranks suggestions.
8. **Smart weighting (وزن‌دهی هوشمند)** — favorited categories weighted up, blocked down.
9. **Daily meal plan (برنامه هفتگی)** — assign dishes to 7 days × 3 meals, persisted.
10. **Rating system (امتیازدهی)** — 1–5 stars per dish, feeds weighting.

## Utility
11. **Share dish (اشتراک‌گذاری)** — share as formatted text via Android share sheet.
12. **Backup & restore (پشتیبان‌گیری)** — export/import favorites + history as JSON.
13. **Random widget (ویجت)** — home-screen widget showing a suggestion, tap to re-roll.
14. **Daily notification (یادآور روزانه)** — scheduled suggestion notification at meal times.
15. **Text-to-speech (خواندن با صدا)** — read dish name/ingredients aloud (Farsi TTS).

## Discovery
16. **Surprise me / random dish of the day (غذای روز)** — deterministic daily pick shared by all users.
17. **Recently viewed (اخیراً دیده‌شده)** — quick access to last N viewed dishes.
18. **Category browse screen (مرور دسته‌ها)** — grid of 27 categories → filtered list.
19. **Nutrition info (ارزش غذایی)** — per-dish calories/protein/carbs (offline table).
20. **Unit conversion (تبدیل واحد)** — grams ↔ cups/spoons for ingredient amounts.

## Polish
21. **Onboarding (آموزش اولیه)** — 3-page intro on first launch.
22. **Empty states + haptics (بازخورد لمسی)** — consistent empty states, haptic on roll/favorite.
23. **Search history (تاریخچه جستجو)** — recent queries, tappable.
24. **Accessibility (دسترسی‌پذیری)** — content descriptions, large-font support, TalkBack labels.

## Deliberately skipped
- Cloud sync / accounts — violates offline + no-account principle.
- AI image generation / paid nutrition APIs — cost + offline requirement.
- Social feed — out of scope, adds backend.
