package com.erfanbagheri.tahdig.util

/**
 * Voice-command matcher for hands-free cook mode (#94). Pure and JVM-only — no
 * speech APIs — so alias matching + PersianText normalization edge cases are
 * unit-testable without a recognizer.
 *
 * Matching strategy: the transcript is normalized (ZWNJ stripped, Arabic letters
 * folded, whitespace collapsed) then compared against an alias table where
 * multi-word commands («مرحله بعد») win over their first token («بعدی»).
 * Garbage in → null out; the caller shows «متوجه نشدم» without changing state.
 */
object CookVoiceCommands {

    enum class Command { NEXT, PREVIOUS, REPEAT, TIMER, STOP, DONE }

    /** Longest alias first so «مرحله بعد» is tried before «بعدی». */
    private val ALIASES: List<Pair<String, Command>> = listOf(
        // NEXT
        "مرحله بعد" to Command.NEXT,
        "قدم بعد" to Command.NEXT,
        "بعدی" to Command.NEXT,
        "بعد" to Command.NEXT,
        // PREVIOUS
        "مرحله قبل" to Command.PREVIOUS,
        "قدم قبل" to Command.PREVIOUS,
        "مرحله قبلی" to Command.PREVIOUS,
        "قبلی" to Command.PREVIOUS,
        "قبل" to Command.PREVIOUS,
        // REPEAT — read the current step again.
        "تکرار" to Command.REPEAT,
        "دوباره" to Command.REPEAT,
        "باز بگو" to Command.REPEAT,
        // TIMER — toggle the inline timer (start/pause).
        "تایمر" to Command.TIMER,
        "شروع تایمر" to Command.TIMER,
        // STOP — pause the timer.
        "متوقف" to Command.STOP,
        "توقف" to Command.STOP,
        "استوپ" to Command.STOP,
        // DONE — finish the cook session (exit the mode).
        "تمام شد" to Command.DONE,
        "تمام" to Command.DONE,
    )

    /**
     * Match a raw transcript to a command, or null when nothing is recognized.
     * Exact match first, then prefix («تایمر شروع کن»), so recognizer padding
     * (trailing «لطفا», punctuation) does not kill a valid command.
     */
    fun match(transcript: String?): Command? {
        // ZWNJ usually STANDS FOR a space («مرحله‌بعد» = «مرحله بعد»), but
        // normalize STRIPS it, gluing the words. Pre-fold it to a space here;
        // the rest of the pipeline (Arabic folds, harakat, collapse) still runs.
        val t = PersianText.normalize((transcript ?: return null).replace('\u200C', ' '))
        if (t.isEmpty()) return null
        for ((alias, cmd) in ALIASES) {
            val a = PersianText.normalize(alias)
            if (t == a || t.startsWith("$a ")) return cmd
        }
        return null
    }

    /** Large transient echo text shown for a recognized command (feedback, not state). */
    fun echoOf(cmd: Command): String = when (cmd) {
        Command.NEXT -> "مرحله بعد"
        Command.PREVIOUS -> "مرحله قبل"
        Command.REPEAT -> "تکرار"
        Command.TIMER -> "تایمر"
        Command.STOP -> "توقف"
        Command.DONE -> "تمام شد"
    }

    const val UNKNOWN_ECHO = "متوجه نشدم"
}
