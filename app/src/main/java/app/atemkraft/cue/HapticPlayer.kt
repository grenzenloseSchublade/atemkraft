package app.atemkraft.cue

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Echte Geräte-Vibration je Phasenwechsel (Vibrator-API, benötigt VIBRATE-Permission).
 * Bewusst dezent: kurzer Impuls je Phase, etwas längeres Doppel-Muster am Ende.
 *
 * Wichtig: Mit expliziter [AudioAttributes]-Usage gespielt, damit die Vibration NICHT am
 * System-Schalter „Tipp-Vibration/Haptik" hängt (sonst auf vielen Geräten still verworfen).
 */
class HapticPlayer(context: Context) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** Sanftes UI-Tick (Kreis-Tap): kurz und leise – Bestätigung, kein Cue. */
    fun tick() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        @Suppress("DEPRECATION")
        v.vibrate(VibrationEffect.createOneShot(25, 130), attributes)
    }

    fun play(event: CueEvent) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = when (event) {
            // Volle Amplitude + längere Impulse → deutlich spürbar (auch ohne Amplituden-Steuerung).
            CueEvent.INHALE, CueEvent.EXHALE -> VibrationEffect.createOneShot(110, 255)
            CueEvent.HOLD -> VibrationEffect.createOneShot(70, 255)
            CueEvent.FINISH -> VibrationEffect.createWaveform(longArrayOf(0, 200, 120, 280), intArrayOf(0, 255, 0, 255), -1)
        }
        @Suppress("DEPRECATION")
        v.vibrate(effect, attributes)
    }
}
