package com.erfanbagheri.tahdig.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent

/**
 * Dictation for the ingredient fields, via the device's own speech recogniser.
 *
 * Uses [RecognizerIntent] rather than `SpeechRecognizer` directly: the recogniser app
 * owns the microphone, so this needs no RECORD_AUDIO permission and no runtime prompt.
 * Farsi only — the app is Farsi-only, and a mixed-locale recogniser mangles ingredient
 * names. Returns null when no recogniser is installed (common on bare emulators).
 */
object VoiceInput {

    private const val LOCALE_FA = "fa-IR"

    /** Intent for [ActivityResultContracts.StartActivityForResult], or null if unsupported. */
    fun intent(context: Context): Intent? {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, LOCALE_FA)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, LOCALE_FA)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "مواد را بگو")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        return if (i.resolveActivity(context.packageManager) != null) i else null
    }

    /**
     * First transcript from the recogniser's EXTRA_RESULTS list, cleaned for the
     * comma-separated field. Recognisers habitually append a full stop and pad with
     * spaces. Takes the list rather than the Intent so it stays unit-testable —
     * android.jar stubs return null for every Intent getter on the JVM.
     */
    fun parse(results: List<String>?): String? = results
        ?.firstOrNull()
        ?.trim()
        ?.trimEnd('.', '،', ',', ' ', '!', '؟', '?')
        ?.takeIf { it.isNotEmpty() }

    /** Pull the transcript list out of a recognition result Intent. */
    fun resultsFrom(data: Intent?): List<String>? =
        data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

    /** Append [spoken] to a comma-separated [existing] value. */
    fun append(existing: String, spoken: String): String =
        if (existing.isBlank()) spoken else "${existing.trimEnd('،', ',', ' ')}، $spoken"

    /** True when this device has no recogniser, so the caller can hide the mic button. */
    fun unavailable(context: Context): Boolean = intent(context) == null
}
