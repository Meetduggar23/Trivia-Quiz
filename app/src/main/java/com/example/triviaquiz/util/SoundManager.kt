package com.example.triviaquiz.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.triviaquiz.data.QuizPreferences

/**
 * Manages sound and vibration feedback for the quiz.
 * Respects user settings from QuizPreferences.
 */
class SoundManager(private val context: Context, private val prefs: QuizPreferences) {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    /**
     * Plays a positive tone for correct answers.
     */
    fun playCorrectSound() {
        if (!prefs.isSoundEnabled()) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        } catch (_: Exception) {}
    }

    /**
     * Plays a negative tone for wrong answers.
     */
    fun playWrongSound() {
        if (!prefs.isSoundEnabled()) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 300)
        } catch (_: Exception) {}
    }

    /**
     * Provides short vibration feedback.
     * @param isCorrect True for correct answer pattern, false for wrong.
     */
    fun vibrate(isCorrect: Boolean) {
        if (!prefs.isVibrationEnabled()) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isCorrect) {
                    // Short double pulse for correct
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1))
                } else {
                    // Single longer buzz for wrong
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                // Pre-O (API 24-25) fallback — no VibrationEffect support
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (isCorrect) 150L else 200L)
            }
        } catch (e: SecurityException) {
            // Vibration permission denied — fail silently
        } catch (_: Exception) {}
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
