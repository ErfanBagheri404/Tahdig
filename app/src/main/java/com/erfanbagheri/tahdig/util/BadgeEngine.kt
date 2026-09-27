package com.erfanbagheri.tahdig.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Achievement badges (#121).
 *
 * Every badge is computable from what is ALREADY stored — the cook history,
 * the streak, the prep times on the food rows. No new tables, no server.
 *
 * The engine is pure: [evaluate] takes a snapshot and returns a set of ids.
 * Re-running it on an unchanged history returns the same set, which is what
 * makes the "unlock fires once" rule true by construction rather than by a
 * separate "have I shown this before" bookkeeping table.
 */
object BadgeEngine {

    /** The snapshot the engine judges. Built once from the DB, then reused. */
    data class History(
        /** Every cook event. A dish cooked 3 times appears 3 times. */
        val cooks: List<Cook> = emptyList(),
        /** Distinct dishes ever cooked → their category id (coverage badges). */
        val distinctDishes: List<DistinctDish> = emptyList(),
        /** Total categories in the library — the denominator for «همه دسته‌ها». */
        val totalCategories: Int = 0,
        /** Total cuisine/region values in the library. */
        val totalCuisines: Int = 0,
        /** Longest streak ever achieved, from [StreakMath]. */
        val longestStreak: Int = 0,
        /** Freeze tokens ever GRANTED (not the current bank) — the «بدون فریز» badge. */
        val freezesGranted: Int = 0,
        /** Consecutive days the water target was met (#115) — the «آبرو» badge. */
        val waterGoalDayStreak: Int = 0,
    )

    data class Cook(
        val foodId: Long,
        val at: LocalDate,
        /** Local hour of the cook, 0..23 — the night-shift badges need it. */
        val hour: Int = 12,
        /** Prep minutes of the dish, for the speed-record badge. */
        val prepTimeMin: Int = 0,
        /** A journal note was written for this cook (#124) — the «نویسنده» badge. */
        val hasNote: Boolean = false,
    )

    data class DistinctDish(
        val id: Long,
        val categoryId: Int,
        val cuisine: String = "",
        val prepTimeMin: Int = 0,
    )

    /** A badge definition. [goal] is shown to locked users, so it is data. */
    data class Def(
        val id: String,
        val title: String,
        val requirement: String,
        val emoji: String,
    )

    /** Progress + unlock state for one badge, as the Settings screen renders it. */
    data class State(
        val def: Def,
        val unlocked: Boolean,
        /** Current count toward the goal — the «۳ از ۱۰» line. */
        val progress: Int,
        /** Required count, or 0 for the badges with no counter («همه مناطق»). */
        val goal: Int,
    )

    // ── The collection (~30) ────────────────────────────────────────────

    val DEFS: List<Def> = listOf(
        Def("first_cook", "اولین غذا", "یک بار پختی", "🍳"),
        Def("cook_10", "شروع جدی", "۱۰ بار پختی", "🍽️"),
        Def("cook_50", "آشپز ماهر", "۵۰ بار پختی", "👨‍🍳"),
        Def("cook_100", "سرآشپز", "۱۰۰ بار پختی", "👩‍🍳"),
        Def("cook_250", "استاد آشپزی", "۲۵۰ بار پختی", "🏅"),
        Def("cook_500", "نگهبان آشپزخانه", "۵۰۰ بار پختی", "🏆"),
        Def("distinct_10", "کنجکاو", "۱۰ غذای متفاوت", "🔍"),
        Def("distinct_25", "همه‌چشم", "۲۵ غذای متفاوت", "👀"),
        Def("distinct_50", "شکمو", "۵۰ غذای متفاوت", "🍲"),
        Def("distinct_100", "متفاوت‌خور", "۱۰۰ غذای متفاوت", "🗺️"),
        Def("distinct_250", "سفره‌گسترده", "۲۵۰ غذای متفاوت", "🌈"),
        Def("cat_3", "شروع تنوع", "۳ دسته غذایی", "🥘"),
        Def("cat_5", "تنوع‌پذیر", "۵ دسته غذایی", "🍛"),
        Def("cat_10", "همه‌دست", "۱۰ دسته غذایی", "🧺"),
        Def("cat_all", "همه دسته‌ها", "هر «{n}» دسته غذایی", "🗄️"),
        Def("streak_3", "سه‌روزه", "۳ روز پیاپی", "🔥"),
        Def("streak_7", "هفتگی", "۷ روز پیاپی", "📅"),
        Def("streak_14", "دو هفته", "۱۴ روز پیاپی", "🗓️"),
        Def("streak_30", "یک ماه", "۳۰ روز پیاپی", "🌙"),
        Def("streak_100", "صد روز", "۱۰۰ روز پیاپی", "💯"),
        Def("speed_15", "سریع‌پخت", "یک غذای زیر ۱۵ دقیقه", "⚡"),
        Def("speed_10", "فلش", "یک غذای زیر ۱۰ دقیقه", "🚀"),
        Def("speed_5", "انفجاری", "یک غذای زیر ۵ دقیقه", "💨"),
        Def("night_20", "شیفت شب", "۲۰ پخت بین ۱ بامداد تا ۵ صبح", "🦉"),
        Def("night_50", "شب‌زنده", "۵۰ پخت بین ۱ بامداد تا ۵ صبح", "🌙"),
        Def("no_freeze_month", "بی‌فریز", "یک ماه کامل بدون فریز", "🧊"),
        Def("all_cuisine_1", "همه مناطق", "همه «{n}» منطقه غذایی", "🌍"),
        Def("all_cuisine_2", "جهان‌گرد", "همه «{n}» منطقه غذایی + ۵۰ غذای متفاوت", "🌏"),
        Def("yalda", "یلدایی", "پخت یک غذای یلدایی در دی یا شب چلدر", "🍉"),
        Def("journal_10", "نویسنده", "۱۰ یادداشت در دفتر آشپزی", "✍️"),
        Def("water_7", "آبرو", "۷ روز پیاپی رسیدن به هدف آب", "💧"),
    )

    // ── Evaluation ──────────────────────────────────────────────────────

    /**
     * The badge ids unlocked by this history. Idempotent: the result depends
     * only on the snapshot, so re-evaluating after a re-render, an app-open or
     * a duplicate cook event cannot unlock anything twice.
     */
    fun evaluate(h: History): Set<String> {
        val out = LinkedHashSet<String>()
        val cooks = h.cooks
        val distinct = h.distinctDishes

        fun count(id: String, n: Int, goal: Int) {
            if (n >= goal) out += id
        }

        count("first_cook", cooks.size, 1)
        count("cook_10", cooks.size, 10)
        count("cook_50", cooks.size, 50)
        count("cook_100", cooks.size, 100)
        count("cook_250", cooks.size, 250)
        count("cook_500", cooks.size, 500)

        count("distinct_10", distinct.size, 10)
        count("distinct_25", distinct.size, 25)
        count("distinct_50", distinct.size, 50)
        count("distinct_100", distinct.size, 100)
        count("distinct_250", distinct.size, 250)

        val cats = distinct.map { it.categoryId }.toSet()
        count("cat_3", cats.size, 3)
        count("cat_5", cats.size, 5)
        count("cat_10", cats.size, 10)
        if (h.totalCategories > 0 && cats.size >= h.totalCategories) out += "cat_all"

        count("streak_3", h.longestStreak, 3)
        count("streak_7", h.longestStreak, 7)
        count("streak_14", h.longestStreak, 14)
        count("streak_30", h.longestStreak, 30)
        count("streak_100", h.longestStreak, 100)

        // Speed record: LOWER is better, so the shared `count` helper
        // (n >= goal) points the wrong way — an 8-minute dish would unlock
        // «زیر ۵ دقیقه». Compare newest-first against each threshold.
        val fastest = distinct.filter { it.prepTimeMin > 0 }.minOfOrNull { it.prepTimeMin }
        if (fastest != null) {
            if (fastest <= 15) out += "speed_15"
            if (fastest <= 10) out += "speed_10"
            if (fastest <= 5) out += "speed_5"
        }

        // Cuisines: distinct NON-EMPTY values only — an empty column is
        // "unknown", and counting it as a region would unlock the «همه مناطق»
        // badge for a user who has never left one region.
        val cuisines = distinct.map { it.cuisine.trim() }.filter { it.isNotEmpty() }.toSet()
        if (h.totalCuisines > 0 && cuisines.size >= h.totalCuisines) {
            out += "all_cuisine_1"
            if (distinct.size >= 50) out += "all_cuisine_2"
        }

        // «بی‌فریز»: a full month of 31 days with no gap and no freeze spent.
        // A 30-cook history that is NOT contiguous must NOT unlock it.
        if (spansWholeMonthWithoutFreeze(cooks, h.freezesGranted)) out += "no_freeze_month"

        // Night shift: cooks logged between 01:00 and 05:00. Two tiers so the
        // badge rewards a habit rather than one late night.
        val nightCooks = cooks.count { it.hour in 1..4 }
        count("night_20", nightCooks, 20)
        count("night_50", nightCooks, 50)

        // «نویسنده»: journal notes actually written, not journal rows (every
        // cook creates a row; only some carry a note).
        count("journal_10", cooks.count { it.hasNote }, 10)

        // «آبرو»: a straight run of days meeting the water goal. The streak
        // is computed from the water tables where the data lives, not here.
        count("water_7", h.waterGoalDayStreak, 7)

        // شب یلدا = 30 آذر, which lands on Dec 20–22 Gregorian depending on
        // the year's Nowruz offset — the 3-day window covers every alignment
        // without a full Persian calendar in the binary.
        if (cooks.any { it.at.monthValue == 12 && it.at.dayOfMonth in 20..22 }) {
            out += "yalda"
        }

        return out
    }

    /**
     * The union of distinct cooked days must cover 31 consecutive days and
     * [freezesGranted] must be 0. A 31-day span with a hole fails, because a
     * hole means a freeze paid for it.
     */
    private fun spansWholeMonthWithoutFreeze(
        cooks: List<Cook>,
        freezesGranted: Int,
    ): Boolean {
        if (freezesGranted > 0) return false
        val days = cooks.map { it.at }.toSortedSet()
        if (days.size < 31) return false
        var cursor = days.first()
        for (d in days) {
            if (d != cursor) return false // a hole: the streak needed a freeze
            cursor = cursor.plusDays(1)
        }
        return true
    }

    /**
     * Per-badge state for the Settings «نشان‌ها» list: the counter is the
     * same number the unlock test used, so a locked row always shows real
     * progress instead of a decorative bar.
     */
    fun states(h: History): List<State> {
        val unlocked = evaluate(h)
        val cooks = h.cooks.size
        val distinct = h.distinctDishes.size
        val cats = h.distinctDishes.map { it.categoryId }.toSet().size
        val fastest = h.distinctDishes.filter { it.prepTimeMin > 0 }.minOfOrNull { it.prepTimeMin }
        val cuisines = h.distinctDishes.map { it.cuisine.trim() }
            .filter { it.isNotEmpty() }.toSet().size
        val total = h.totalCategories
        val nightCooks = h.cooks.count { it.hour in 1..4 }
        val journalNotes = h.cooks.count { it.hasNote }

        return DEFS.map { d ->
            val (p, g) = when (d.id) {
                "first_cook" -> cooks to 1
                "cook_10" -> cooks to 10
                "cook_50" -> cooks to 50
                "cook_100" -> cooks to 100
                "cook_250" -> cooks to 250
                "cook_500" -> cooks to 500
                "distinct_10" -> distinct to 10
                "distinct_25" -> distinct to 25
                "distinct_50" -> distinct to 50
                "distinct_100" -> distinct to 100
                "distinct_250" -> distinct to 250
                "cat_3" -> cats to 3
                "cat_5" -> cats to 5
                "cat_10" -> cats to 10
                "cat_all" -> cats to h.totalCategories
                "streak_3" -> h.longestStreak to 3
                "streak_7" -> h.longestStreak to 7
                "streak_14" -> h.longestStreak to 14
                "streak_30" -> h.longestStreak to 30
                "streak_100" -> h.longestStreak to 100
                // Progress for a record reads as «سریع‌ترین: ۸ دقیقه» — the
                // goal is the threshold, the progress is the achieved time.
                "speed_15" -> (fastest ?: 0) to 15
                "speed_10" -> (fastest ?: 0) to 10
                "speed_5" -> (fastest ?: 0) to 5
                "all_cuisine_1" -> cuisines to h.totalCuisines
                "all_cuisine_2" -> cuisines to h.totalCuisines
                "night_20" -> nightCooks to 20
                "night_50" -> nightCooks to 50
                "journal_10" -> journalNotes to 10
                "water_7" -> h.waterGoalDayStreak to 7
                // «بی‌فریز» and «یلدا» have no meaningful counter — the
                // requirement text carries the rule.
                else -> 0 to 0
            }
            State(
                // A record badge's requirement must name the achieved number,
                // otherwise «زیر ۱۵» sits next to «۱۲» and reads as a fail.
                def = when {
                    d.id.startsWith("speed_") && (fastest ?: 0) > 0 ->
                        d.copy(requirement = "سریع‌ترین: $fastest دقیقه")
                    // The «{n}» in the library-wide badges is the denominator,
                    // which only the snapshot knows — fill it or the row says
                    // «همه «{n}» دسته» on screen.
                    d.id == "cat_all" -> d.copy(
                        requirement = "هر $total دسته غذایی",
                    )
                    d.id == "all_cuisine_1" -> d.copy(
                        requirement = "همه ${h.totalCuisines} منطقه غذایی",
                    )
                    d.id == "all_cuisine_2" -> d.copy(
                        requirement = "همه ${h.totalCuisines} منطقه + ۵۰ غذای متفاوت",
                    )
                    else -> d
                },
                unlocked = d.id in unlocked,
                progress = p,
                goal = g,
            )
        }
    }

    /** Badges newly present in [next] that were not in [previous] — the toast. */
    fun newlyUnlocked(previous: Set<String>, next: Set<String>): List<Def> =
        (next - previous).mapNotNull { fresh -> DEFS.firstOrNull { it.id == fresh } }
            .sortedBy { DEFS.indexOf(it) }
}
