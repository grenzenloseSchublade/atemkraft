package app.atemkraft.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.atemkraft.data.IntervalOverrides
import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.domain.isRoundBased
import kotlin.math.roundToInt

/**
 * Vorgaben einer Übung für die Anpassung auf der Detailseite: Runden bzw. Minuten und die
 * Sekunden der festen Phasen. null = die Übung hat diese Phase nicht mit fester Dauer, dann gibt
 * es dafür keinen Stepper.
 */
data class AdjustDefaults(
    val roundBased: Boolean,
    val duration: Int,
    val inhale: Int?,
    val hold: Int?,
    val exhale: Int?,
) {
    /** Grenzen des Dauer-Steppers: Runden oder Minuten. */
    val durationRange: IntRange get() = if (roundBased) AdjustLimits.ROUNDS else AdjustLimits.MINUTES

    companion object {
        fun of(exercise: Exercise): AdjustDefaults {
            val phases = exercise.segments.flatMap { it.phases }
            fun seconds(type: PhaseType): Int? = phases.firstNotNullOfOrNull { phase ->
                (phase.duration as? PhaseDuration.Fixed)?.takeIf { phase.type == type }
            }?.let { (it.millis / 1000.0).roundToInt() }
            return AdjustDefaults(
                roundBased = exercise.isRoundBased,
                duration = if (exercise.isRoundBased) exercise.rounds else exercise.defaultMinutes(),
                inhale = seconds(PhaseType.INHALE),
                hold = seconds(PhaseType.HOLD_FULL) ?: seconds(PhaseType.HOLD_EMPTY),
                exhale = seconds(PhaseType.EXHALE),
            )
        }
    }
}

/**
 * Anpassung einer Übung auf der Detailseite (Dauer und Phasenlängen), getrennt von der
 * Darstellung – nach dem Vorbild von `DurationPickerState` (ARC-7). Die Seite liest und ändert
 * nur diesen Zustand.
 *
 * „Angepasst“ ist kein Merker, sondern der Wertvergleich mit den [defaults]: Wer manuell auf die
 * Vorgaben zurücksteppt, gilt wieder als unangepasst ([overrides] = null, die gespeicherte
 * Anpassung wird gelöscht).
 */
@Stable
class SessionAdjustState(
    val defaults: AdjustDefaults,
    duration: Int = defaults.duration,
    inhale: Int? = defaults.inhale,
    hold: Int? = defaults.hold,
    exhale: Int? = defaults.exhale,
    storedApplied: Boolean = false,
) {
    /** Runden bzw. Minuten. */
    var duration: Int by mutableIntStateOf(duration)

    /** Sekunden der Phasen; null, wenn die Übung die Phase nicht hat (bleibt dann null). */
    var inhale: Int? by mutableStateOf(inhale.takeIf { defaults.inhale != null })
    var hold: Int? by mutableStateOf(hold.takeIf { defaults.hold != null })
    var exhale: Int? by mutableStateOf(exhale.takeIf { defaults.exhale != null })

    /** Wurde die gespeicherte Anpassung schon (einmal) geprüft? */
    var storedApplied: Boolean by mutableStateOf(storedApplied)
        private set

    /** Weicht eine Phasenlänge von der Vorgabe ab? */
    val intervalsModified: Boolean
        get() = inhale != defaults.inhale || hold != defaults.hold || exhale != defaults.exhale

    /** Weicht irgendetwas von der Vorgabe ab (dann zeigt der Start den Reset)? */
    val modified: Boolean get() = duration != defaults.duration || intervalsModified

    /** Was gespeichert wird; null = alles auf Vorgabe, gespeicherte Anpassung löschen. */
    val overrides: IntervalOverrides?
        get() = if (modified) IntervalOverrides(duration, inhale, hold, exhale) else null

    /** Die Anpassung für den Start; Phasenlängen nur, wenn sie von der Vorgabe abweichen. */
    fun config(): SessionConfig {
        val base = if (defaults.roundBased) SessionConfig(rounds = duration) else SessionConfig(minutes = duration)
        return if (intervalsModified) {
            base.copy(
                inhaleSeconds = inhale?.toDouble(),
                exhaleSeconds = exhale?.toDouble(),
                holdSeconds = hold?.toDouble(),
            )
        } else {
            base
        }
    }

    /** Alles zurück auf die Vorgaben der Übung. */
    fun reset() {
        duration = defaults.duration
        inhale = defaults.inhale
        hold = defaults.hold
        exhale = defaults.exhale
    }

    /**
     * Übernimmt die gespeicherte Anpassung genau einmal (sie kommt asynchron aus dem DataStore).
     * Hat der Nutzer bis dahin schon etwas verstellt, hat das Vorrang – sonst schwappten
     * Write-throughs zurück. Geklemmt: Gespeichertes (auch Importiertes) darf die Grenzen der
     * Stepper nicht umgehen.
     */
    fun applyStored(stored: IntervalOverrides) {
        if (storedApplied) return
        storedApplied = true
        if (modified) return
        stored.duration?.let { duration = it.coerceIn(defaults.durationRange) }
        if (defaults.inhale != null) stored.inhale?.let { inhale = it.coerceIn(AdjustLimits.INHALE_S) }
        if (defaults.hold != null) stored.hold?.let { hold = it.coerceIn(AdjustLimits.HOLD_S) }
        if (defaults.exhale != null) stored.exhale?.let { exhale = it.coerceIn(AdjustLimits.EXHALE_S) }
    }

    companion object {
        /**
         * Sichert Werte und den Übernahme-Merker über Konfigurationswechsel und Prozesstod – samt
         * der Vorgaben, zu denen sie gehören. Haben sich die Vorgaben inzwischen geändert (ein
         * gespeichertes Muster wurde im Editor bearbeitet, die Id bleibt), gelten die alten Werte
         * nicht mehr: Der Zustand beginnt neu bei den aktuellen [defaults].
         */
        fun saver(defaults: AdjustDefaults): Saver<SessionAdjustState, *> = listSaver(
            // Fehlende Phasen als 0: Beim Wiederherstellen bleiben sie über [defaults] null.
            save = {
                listOf(it.duration, it.inhale ?: 0, it.hold ?: 0, it.exhale ?: 0, if (it.storedApplied) 1 else 0) +
                    encode(it.defaults)
            },
            restore = {
                if (it.drop(SAVED_VALUES) != encode(defaults)) {
                    SessionAdjustState(defaults)
                } else {
                    SessionAdjustState(defaults, it[0], it[1], it[2], it[3], storedApplied = it[4] == 1)
                }
            },
        )

        private const val SAVED_VALUES = 5
        private const val NO_PHASE = -1

        private fun encode(defaults: AdjustDefaults): List<Int> = listOf(
            if (defaults.roundBased) 1 else 0,
            defaults.duration,
            defaults.inhale ?: NO_PHASE,
            defaults.hold ?: NO_PHASE,
            defaults.exhale ?: NO_PHASE,
        )
    }
}

/**
 * [SessionAdjustState] der Übung über rememberSaveable, geschlüsselt nach ihrer Id und ihren
 * Vorgaben: Ändert sich ein gespeichertes Muster bei gleicher Id (Editor), beginnt die Anpassung
 * neu – offen auf der Seite über den Schlüssel, nach „Zurück“ über den [SessionAdjustState.saver].
 */
@Composable
fun rememberSessionAdjustState(exercise: Exercise): SessionAdjustState {
    val defaults = remember(exercise) { AdjustDefaults.of(exercise) }
    return rememberSaveable(exercise.id, defaults, saver = SessionAdjustState.saver(defaults)) { SessionAdjustState(defaults) }
}
