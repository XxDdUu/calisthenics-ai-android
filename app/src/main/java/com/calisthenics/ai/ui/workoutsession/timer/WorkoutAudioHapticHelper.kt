package com.calisthenics.ai.ui.workoutsession.timer

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class WorkoutAudioHapticHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator: Vibrator? by lazy {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
    }

    private var toneGenerator: ToneGenerator? = null

    init {
        runCatching {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        }
    }

    open fun playCountdownTick(sound: Boolean = true, vibration: Boolean = true) {
        if (sound) playTone(ToneGenerator.TONE_PROP_BEEP, 70)
        if (vibration) vibrate(50)
    }

    open fun playHoldStart(sound: Boolean = true, vibration: Boolean = true) {
        if (sound) playTone(ToneGenerator.TONE_PROP_ACK, 180)
        if (vibration) vibrate(180)
    }

    open fun playHoldStop(sound: Boolean = true, vibration: Boolean = true) {
        if (sound) playTone(ToneGenerator.TONE_PROP_BEEP2, 140)
        if (vibration) vibratePattern(longArrayOf(0, 100, 80, 100))
    }

    open fun playRestCompleted(sound: Boolean = true, vibration: Boolean = true) {
        if (sound) playTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
        if (vibration) vibratePattern(longArrayOf(0, 150, 100, 150, 100, 300))
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        runCatching {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            }
            toneGenerator?.startTone(toneType, durationMs)
        }
    }

    private fun vibrate(durationMs: Long) {
        runCatching {
            val vib = vibrator ?: return
            if (!vib.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        }
    }

    private fun vibratePattern(pattern: LongArray) {
        runCatching {
            val vib = vibrator ?: return
            if (!vib.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, -1)
            }
        }
    }
}

