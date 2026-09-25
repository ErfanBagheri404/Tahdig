package com.erfanbagheri.tahdig.widget

import com.erfanbagheri.tahdig.util.PersianText
import com.erfanbagheri.tahdig.util.StreakMath

/**
 * Pure widget decision layer (#131). Kept free of Android types so the
 * formatting + routing rules are unit-testable on the JVM, the same way
 * [StreakMath] and the other util objects are.
 *
 * The widget surface is three things worth a home screen: *what to eat*, *how
 * long you've kept at it*, and *one tap to change the answer*. Everything
 * here exists to keep those three consistent between the widget and the app.
 */
object WidgetLogic {

    /** Which of the three layouts a widget instance renders. */
    enum class Size { COMPACT, MEDIUM, LARGE }

    /**
     * Layout from the launcher's reported size, in dp (the unit
     * `OPTION_APPWIDGET_MIN_WIDTH/HEIGHT` already arrives in).
     *
     * dp rather than a cell count: the platform's own widget sizing guidance
     * buckets by dp, and guessing cells from dp reintroduces the rounding that
     * made a 2x2 fall into the wrong layout. A launcher cell is ~40dp plus
     * padding, so these thresholds track the standard 2x2 / 4x2 / 4x4 shapes.
     */
    fun sizeFor(widthDp: Int, heightDp: Int): Size = when {
        widthDp >= 280 || heightDp >= 250 -> Size.LARGE
        widthDp >= 180 -> Size.MEDIUM
        else -> Size.COMPACT
    }

    /**
     * The streak line, or null when there is nothing to show.
     *
     * Day 0 is not a streak worth claiming, so it renders nothing rather than
     * «۰ روز» — the widget shouldn't advertise a game the user isn't in.
     */
    fun streakLine(current: Int, frozenDayPresent: Boolean): String? =
        if (current <= 0) null
        else "🔥 ${PersianText.toPersianDigits(current.toString())} روز" +
            if (frozenDayPresent) " (بیفریز)" else ""

    /** Streak line from a full [StreakMath.State]; null when un-started. */
    fun streakLine(state: StreakMath.State): String? =
        streakLine(state.current, state.frozenDay != null)

    /**
     * Where each tap goes. Kept as a sealed-ish set of targets so
     * [DishOfDayWidget] can build intents and the test can assert routing
     * without a Context.
     */
    enum class Action { OPEN_DISH, SHUFFLE, OPEN_HOME, OPEN_JOURNAL }

    /**
     * Route for a tap on a dish name. The 4x4 list taps open that dish's
     * detail; a single dish does the same.
     */
    fun routeForDish(foodId: Long): Action = Action.OPEN_DISH

    /**
     * Which deterministic index a manual spin lands on.
     *
     * The app's dish-of-the-day is `dayOfYear % count` and must stay that way
     * so the widget and the app never disagree. A manual shuffle is
     * *deliberately* allowed to leave it: the user asked for something else.
     * We store a per-day spin offset so repeated spins cycle instead of
     * re-picking, and so a widget update after a spin shows the spun dish
     * rather than snapping back to today's pick.
     */
    fun shuffledIndex(spinCount: Int, count: Int): Int {
        if (count <= 0) return 0
        return ((spinCount % count) + count) % count
    }

    /**
     * Compose the three dish names for the 4x4 list. Drops nulls so a failed
     * lookup shrinks the list instead of rendering a blank row.
     */
    fun listTitles(names: List<String?>, limit: Int = 3): List<String> =
        names.filterNotNull().take(limit)
}
