package com.erfanbagheri.tahdig.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Shake-to-advance (#96). Every decision is a pure function so the sensitivity
 * slider and the false-advance guard are unit-testable without a sensor.
 */
object ShakeDetector {

    /** Slider default — middle of the threshold range. */
    const val DEFAULT_SENSITIVITY = 0.5f

    /** m/s² at sensitivity 0 (needs a hard shake) and 1 (a light nudge). */
    private const val HARD_THRESHOLD = 26f
    private const val LIGHT_THRESHOLD = 11f

    /** Ignore further shakes for this long after one fires (a shake is a burst). */
    const val REARM_MS = 1500L

    /** A running timer at or under [BLOCK_BELOW_SECS] must not be shaken away. */
    const val BLOCK_BELOW_SECS = 60L

    const val GRAVITY = 9.81f

    /** Slider 0..1 → acceleration threshold. Higher sensitivity = lower bar. */
    fun thresholdFor(sensitivity: Float): Float =
        HARD_THRESHOLD - (HARD_THRESHOLD - LIGHT_THRESHOLD) * sensitivity.coerceIn(0f, 1f)

    /** A shake is a spike past the threshold, outside the re-arm window. */
    fun isShake(magnitude: Float, threshold: Float, nowMs: Long, lastShakeMs: Long): Boolean =
        magnitude >= threshold && nowMs - lastShakeMs >= REARM_MS

    /**
     * False-advance guard: never step while a SHORT timer is counting down — the
     * user is watching a deadline, not stirring. A long timer or no timer is fine.
     */
    fun canAdvance(remainingSec: Long?, running: Boolean): Boolean =
        !(running && remainingSec != null && remainingSec <= BLOCK_BELOW_SECS)

    /** Length of the gravity-free acceleration vector. */
    fun magnitude(x: Float, y: Float, z: Float): Float = sqrt(x * x + y * y + z * z)

    /** Raw accelerometer reads include gravity; linear-acceleration does not. */
    fun magnitudeOf(raw: Float, linearSensor: Boolean): Float =
        if (linearSensor) raw else abs(raw - GRAVITY)
}

/**
 * Registers the accelerometer for as long as the cook screen is open. Prefers
 * TYPE_LINEAR_ACCELERATION (gravity already removed) and falls back to the raw
 * accelerometer with gravity subtracted.
 */
class ShakeWatcher(context: Context, private val onShake: () -> Unit) : SensorEventListener {

    private val manager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val linear = manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val sensor = linear ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /** Set from the Settings slider; the caller may also flip [enabled]. */
    var threshold: Float = ShakeDetector.thresholdFor(ShakeDetector.DEFAULT_SENSITIVITY)

    /** Gates the whole detector — the timer guard lives in the caller. */
    var enabled: Boolean = true

    private var lastShakeMs = 0L

    /** True when a usable sensor exists; without one the feature stays silent. */
    val available: Boolean get() = sensor != null

    fun start() {
        val s = sensor ?: return
        manager.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() = manager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        if (!enabled || event.values.size < 3) return
        val raw = ShakeDetector.magnitude(event.values[0], event.values[1], event.values[2])
        val magnitude = ShakeDetector.magnitudeOf(raw, linearSensor = linear != null)
        val now = android.os.SystemClock.elapsedRealtime()
        if (ShakeDetector.isShake(magnitude, threshold, now, lastShakeMs)) {
            lastShakeMs = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
