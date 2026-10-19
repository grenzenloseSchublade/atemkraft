package app.atemkraft.cue

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibrationEffect.Composition.PRIMITIVE_CLICK
import android.os.VibrationEffect.Composition.PRIMITIVE_TICK
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

/**
 * Echte Geräte-Vibration (Vibrator-API, benötigt VIBRATE-Permission): Impuls je Phasenwechsel,
 * Doppel-Muster am Ende, ein sanfter Tick beim Tippen auf den Kreis und die Rasten des Dauer-Reglers.
 *
 * Session-Vibration (Phasen, Ende, Tipp-Tick am Kreis) folgt dem App-Schalter „Vibration“
 * (AUDIO-05): Die Einstellung ist Pflicht-Parameter dieser Methoden, damit keine Aufrufstelle sie
 * vergessen kann. Ausnahme sind die Rasten des Dauer-Reglers ([dialTick]): Sie sind Bedien-
 * Rückmeldung wie ein Drehknopf, kein Session-Cue, und laufen deshalb als Berührungs-Vibration –
 * an/aus und Stärke regelt allein Android (Nutzerentscheidung 2026-10-19).
 *
 * Session-Vibration läuft als Alarm-Vibration, auch der Tipp-Tick, damit es NICHT an der System-Einstellung
 * für Berührungs-Vibration (`Settings.System.HAPTIC_FEEDBACK_ENABLED`) hängt: Ist sie aus,
 * verwerfen manche Geräte (u. a. Samsung) Touch-Vibrationen still, und der Tick wäre trotz
 * eingeschaltetem App-Schalter weg (Nutzerentscheidung 2026-10-08, AUDIO-05). Alarm läuft auch im
 * Energiesparmodus und bei Lautlos; das gilt für jede Effekt-Art (One-Shot, vordefiniert,
 * Primitive) gleich – die Usage entscheidet über Zulassen und Stärke-Einstellung, nicht der
 * Effekt (AOSP `VibrationSettings`, Stand 2026-10). Abschalten lässt sich alles über den
 * App-Schalter. Ab API 33 über [VibrationAttributes], davor über die gleichwertige
 * [AudioAttributes]-Usage.
 *
 * [clock] (Millisekunden, monoton) nur für die Drosselung der Rasten; in Tests injizierbar.
 */
class HapticPlayer(context: Context, private val clock: () -> Long = SystemClock::uptimeMillis) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    // Fähigkeiten des Motors ändern sich nicht: einmal beim ersten Tick ermitteln.
    private val tickKind: TickKind by lazy { tickKindOf(vibrator) }
    private val tickGate = TickGate()

    /** Sanfter Tick beim Tippen auf den Kreis; nur bei eingeschaltetem App-Schalter „Vibration“. */
    fun tick(enabled: Boolean) {
        if (!enabled) return
        raste(strong = false, touch = false)
    }

    /**
     * Raste des Dauer-Reglers: leiser Tick je Minute; [strong]: satter, deutlich abgesetzter Klick
     * (je 5 Minuten, ∞). Unabhängig vom App-Schalter, als Berührungs-Vibration – Android regelt
     * an/aus und Stärke über die System-Einstellung für Berührungs-Feedback.
     *
     * Jeder neue Vibrationsauftrag bricht den laufenden ab (gleiche Usage). Dichte Ticks beim
     * schnellen Ziehen löschten sich so gegenseitig aus, bevor der Motor anlief – am Galaxy A54
     * kam gar nichts an. Darum gedrosselt ([TickGate]); ein Klick geht immer durch.
     *
     * Effekt nach Gerät, vom satten zum einfachen: Primitive ab API 30, falls der Motor sie kann
     * (Tick leise, Klick volle Skala); sonst vordefinierte Effekte (ab API 30 nur, wenn der Motor
     * sie sicher kann – sonst spielt das System eine Ersatz-Wellenform unbekannter Stärke; API 29
     * kann es nicht abfragen und nimmt sie mit Ersatz); sonst One-Shot mit voller Amplitude, lang
     * genug, dass auch ein träger Motor anläuft.
     */
    fun dialTick(strong: Boolean) = raste(strong, touch = true)

    private fun raste(strong: Boolean, touch: Boolean) {
        if (!tickGate.admit(strong, clock())) return
        vibrate(tickEffect(tickKind, strong), touch)
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

    /** [touch]: Berührungs-Vibration (System regelt sie), sonst Alarm-Vibration (App-Schalter). */
    private fun vibrate(effect: VibrationEffect, touch: Boolean) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrateWithUsage(v, effect, if (touch) VibrationAttributes.USAGE_TOUCH else VibrationAttributes.USAGE_ALARM)
        } else {
            // Vor API 33 gibt es nur diesen Overload; das System bildet USAGE_ALARM auf Alarm- und
            // USAGE_ASSISTANCE_SONIFICATION auf Berührungs-Vibration ab.
            @Suppress("DEPRECATION")
            v.vibrate(effect, if (touch) touchAudioAttributes else alarmAudioAttributes)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun vibrateWithUsage(v: Vibrator, effect: VibrationEffect, usage: Int) {
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

/**
 * Drosselung der Rasten: Ein leichter Tick geht nur durch, wenn der vorige ausgeklungen ist
 * ([TICK_GAP_MS] nach einem Tick, [STRONG_GAP_MS] nach einem Klick); ein Klick ([admit] mit
 * `strong`) geht immer durch und hat Vorrang. Zeit [now] in Millisekunden, monoton.
 */
internal class TickGate {

    private var quietUntil = Long.MIN_VALUE

    /** Darf die Raste jetzt spielen? Merkt sie sich dann als zuletzt gespielte. */
    fun admit(strong: Boolean, now: Long): Boolean {
        if (!strong && now < quietUntil) return false
        quietUntil = now + if (strong) STRONG_GAP_MS else TICK_GAP_MS
        return true
    }
}

/** Effekt-Art der Rasten, nach Fähigkeit des Motors. */
private enum class TickKind { PRIMITIVES, PREDEFINED, ONE_SHOT }

private fun tickKindOf(v: Vibrator?): TickKind = when {
    v == null -> TickKind.ONE_SHOT

    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> when {
        supportsPrimitives(v) -> TickKind.PRIMITIVES
        supportsPredefined(v) -> TickKind.PREDEFINED
        else -> TickKind.ONE_SHOT
    }

    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> TickKind.PREDEFINED

    else -> TickKind.ONE_SHOT
}

// Die Abfragen gehen über den Vibrator-Dienst des Herstellers; scheitert eine, bleibt der
// sichere One-Shot statt gar keiner Raste.
@RequiresApi(Build.VERSION_CODES.R)
private fun supportsPrimitives(v: Vibrator): Boolean = runCatching { v.areAllPrimitivesSupported(PRIMITIVE_TICK, PRIMITIVE_CLICK) }.getOrDefault(false)

@RequiresApi(Build.VERSION_CODES.R)
private fun supportsPredefined(v: Vibrator): Boolean = runCatching {
    v.areAllEffectsSupported(VibrationEffect.EFFECT_TICK, VibrationEffect.EFFECT_HEAVY_CLICK) == Vibrator.VIBRATION_EFFECT_SUPPORT_YES
}.getOrDefault(false)

private fun tickEffect(kind: TickKind, strong: Boolean): VibrationEffect = when {
    kind == TickKind.PRIMITIVES && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> primitiveTick(strong)
    kind == TickKind.PREDEFINED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> predefinedTick(strong)
    else -> VibrationEffect.createOneShot(if (strong) STRONG_ONE_SHOT_MS else TICK_ONE_SHOT_MS, FULL_AMPLITUDE)
}

@RequiresApi(Build.VERSION_CODES.R)
private fun primitiveTick(strong: Boolean): VibrationEffect {
    val primitive = if (strong) PRIMITIVE_CLICK else PRIMITIVE_TICK
    val scale = if (strong) STRONG_PRIMITIVE_SCALE else TICK_PRIMITIVE_SCALE
    return VibrationEffect.startComposition().addPrimitive(primitive, scale).compose()
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun predefinedTick(strong: Boolean): VibrationEffect = VibrationEffect.createPredefined(if (strong) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_TICK)

// Mindestabstand nach einem Tick bzw. Klick (länger als der One-Shot, damit nichts abreißt).
internal const val TICK_GAP_MS = 40L
internal const val STRONG_GAP_MS = 80L

// One-Shot-Rasten: kurz, aber lang genug für träge Motoren (25 ms @130 kam am A54 nicht an).
internal const val TICK_ONE_SHOT_MS = 30L
internal const val STRONG_ONE_SHOT_MS = 60L
private const val FULL_AMPLITUDE = 255

// Primitive: der Tick leise und klar vom Klick abgesetzt, der Klick mit voller Skala.
internal const val TICK_PRIMITIVE_SCALE = 0.6f
internal const val STRONG_PRIMITIVE_SCALE = 1f
