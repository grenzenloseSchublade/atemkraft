package app.atemkraft.cue

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

/**
 * Echte Geräte-Vibration (Vibrator-API, benötigt VIBRATE-Permission): Impuls je Phasenwechsel,
 * Doppel-Muster am Ende und ein sanfter Tick beim Tippen auf den Kreis.
 *
 * Jede Vibration folgt dem App-Schalter „Vibration“ (AUDIO-05): Die Einstellung ist Pflicht-
 * Parameter jeder Methode, damit keine Aufrufstelle sie vergessen kann.
 *
 * Phasen-Haptik läuft als Alarm-Vibration, damit sie NICHT an der System-Einstellung für
 * Berührungs-Vibration (`Settings.System.HAPTIC_FEEDBACK_ENABLED`) hängt – sie ist ein Cue,
 * keine Bedien-Rückmeldung. Der Tipp-Tick ist dagegen Touch-Feedback (AUDIO-05) und folgt
 * auch dieser System-Einstellung. Ab API 33 über [VibrationAttributes], davor über die
 * gleichwertigen [AudioAttributes]-Usages.
 */
class HapticPlayer(context: Context) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    /** Sanftes UI-Tick (Kreis-Tap): kurz und leise – Bestätigung, kein Cue. */
    fun tick(enabled: Boolean) {
        if (!enabled) return
        vibrate(VibrationEffect.createOneShot(25, 130), touch = true)
    }

    /** Phasen- und End-Haptik; nur bei eingeschaltetem App-Schalter „Vibration“. */
    fun play(event: CueEvent, enabled: Boolean) {
        if (!enabled) return
        val effect = when (event) {
            // Volle Amplitude + längere Impulse → deutlich spürbar (auch ohne Amplituden-Steuerung).
            CueEvent.INHALE, CueEvent.EXHALE -> VibrationEffect.createOneShot(110, 255)

            CueEvent.HOLD -> VibrationEffect.createOneShot(70, 255)

            CueEvent.FINISH -> VibrationEffect.createWaveform(longArrayOf(0, 200, 120, 280), intArrayOf(0, 255, 0, 255), -1)
        }
        vibrate(effect, touch = false)
    }

    private fun vibrate(effect: VibrationEffect, touch: Boolean) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrateWithAttributes(v, effect, touch)
        } else {
            // Vor API 33 gibt es nur diesen Overload; das System bildet ASSISTANCE_SONIFICATION
            // auf Touch- und ALARM auf Alarm-Vibration ab.
            @Suppress("DEPRECATION")
            v.vibrate(effect, if (touch) touchAudioAttributes else alarmAudioAttributes)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun vibrateWithAttributes(v: Vibrator, effect: VibrationEffect, touch: Boolean) {
        val usage = if (touch) VibrationAttributes.USAGE_TOUCH else VibrationAttributes.USAGE_ALARM
        v.vibrate(effect, VibrationAttributes.createForUsage(usage))
    }

    private companion object {
        val alarmAudioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val touchAudioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
