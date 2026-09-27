package com.erfanbagheri.tahdig.util

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Streaming recognizer for hands-free cook mode (#94): a listen → result → listen
 * loop held only while the mode's voice toggle is on.
 *
 * Unlike [VoiceInput] (intent-based, one-shot, recognizer app owns the mic, no
 * permission), holding a streaming session ourselves DOES require RECORD_AUDIO —
 * the caller requests it before [start]. [release] cancels and destroys the
 * recognizer so no mic is held after the toggle turns off (AC: toggle off →
 * session fully released).
 *
 * Created and destroyed on the main thread (Compose effects run there);
 * recognition callbacks arrive on that same looper, so restarting the loop from
 * a callback is safe.
 *
 * @param onTranscript top result of each utterance, or null on no-speech errors.
 */
class CookVoiceSession(
    private val context: android.content.Context,
    private val onTranscript: (String?) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    private val listener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            onTranscript(text)
            restart()
        }

        override fun onError(error: Int) {
            // No-match/timeout are the normal outcome of background noise — keep
            // listening. A missing mic permission would hot-loop forever, so that
            // one stops the session (the caller must have granted it first).
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                listening = false
                return
            }
            if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            ) {
                onTranscript(null)   // silence/noise → «متوجه نشدم», no state change
            }
            restart()
        }

        // Unused: the session is result-driven, no partials or levels needed.
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    fun start() {
        if (listening) return
        listening = true
        listen()
    }

    private fun restart() {
        if (listening) listen()
    }

    private fun listen() {
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context)
            .also { recognizer = it }
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            },
        )
    }

    /** AC: toggle off → recognizer fully released, no mic held. Idempotent. */
    fun release() {
        listening = false
        val r = recognizer ?: return
        recognizer = null
        runCatching { r.cancel() }
        r.destroy()
    }
}
