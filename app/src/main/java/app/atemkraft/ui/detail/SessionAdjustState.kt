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
import app.atemkraft.domain.AdjustDefaults
import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.SessionConfig

/**
 * Anpassung einer Übung auf der Detailseite (Dauer, Phasenlängen, „Danach Stille“), getrennt von
 * der Darstellung – nach dem Vorbild von `DurationPickerState` (ARC-7). Die Seite liest und ändert
 * nur diesen Zustand.
 *
 * „Angepasst“ ist kein Merker, sondern der Wertvergleich mit den [defaults]: Wer manuell auf die
 * Vorgaben zurücksteppt, gilt wieder als unangepasst ([overrides] = null, die gespeicherte
 * Anpassung wird gelöscht).
 *
 * „Danach Stille“ (Eigentümer 2026-11-24): Die Vorgabe kommt nicht aus der Übung, sondern aus den
 * Einstellungen ([silenceDefault], setzt die Seite, sobald sie geladen sind; null = Übung ohne
 * Stille danach oder noch nicht geladen, dann keine Zeile und keine Wahl im Start). [silence] ist
 * nur die eigene Wahl; null = folgt der Vorgabe, auch wenn diese sich später ändert.
 */
@Stable
class SessionAdjustState(
    val defaults: AdjustDefaults,
    duration: Int = defaults.duration,
    inhale: Int? = defaults.inhale,
    hold: Int? = defaults.hold,
    exhale: Int? = defaults.exhale,
    silence: Int? = null,
    storedApplied: Boolean = false,
) {
    /** Runden bzw. Minuten. */
    var duration: Int by mutableIntStateOf(duration)

    /** Sekunden der Phasen; null, wenn die Übung die Phase nicht hat (bleibt dann null). */
    var inhale: Int? by mutableStateOf(inhale.takeIf { defaults.inhale != null })
    var hold: Int? by mutableStateOf(hold.takeIf { defaults.hold != null })
    var exhale: Int? by mutableStateOf(exhale.takeIf { defaults.exhale != null })

    /** Eigene Wahl „Danach Stille“ in Minuten (0 = aus); null = Vorgabe ([silenceDefault]). */
    var silence: Int? by mutableStateOf(silence)

    /**
     * Vorgabe „Danach Stille“ aus den Einstellungen: aus (0) oder die Dauer des Tabs Meditation;
     * null = keine Stille-Zeile (nicht erlaubt bzw. noch nicht geladen).
     */
    var silenceDefault: Int? by mutableStateOf(null)

    /** Was gilt: eigene Wahl oder Vorgabe; null ohne Stille-Zeile. */
    val silenceMinutes: Int?
        get() = silenceDefault?.let { default -> silence ?: default }

    /** Weicht „Danach Stille“ von der Vorgabe ab? */
    val silenceModified: Boolean
        get() = silenceDefault != null && silence != null && silence != silenceDefault

    /** Setzt „Danach Stille“; der Wert der Vorgabe heißt wieder „folgt der Vorgabe“. */
    fun selectSilence(minutes: Int) {
        silence = minutes.takeIf { it != silenceDefault }
    }

    /** Wurde die gespeicherte Anpassung schon (einmal) geprüft? */
    var storedApplied: Boolean by mutableStateOf(storedApplied)
        private set

    /** Weicht eine Phasenlänge von der Vorgabe ab? */
    val intervalsModified: Boolean
        get() = inhale != defaults.inhale || hold != defaults.hold || exhale != defaults.exhale

    /** Weicht irgendetwas von der Vorgabe ab (dann zeigt der Start den Reset)? */
    val modified: Boolean get() = duration != defaults.duration || intervalsModified || silenceModified

    /** Was gespeichert wird; null = alles auf Vorgabe, gespeicherte Anpassung löschen. */
    val overrides: IntervalOverrides?
        get() = if (modified) IntervalOverrides(duration, inhale, hold, exhale, keptSilence) else null

    /**
     * Was von „Danach Stille“ gespeichert bleibt: die eigene Wahl, solange sie von der Vorgabe
     * abweicht – oder unverändert, solange die Vorgabe fehlt (eine Dauer-Änderung vor dem Laden der
     * Einstellungen löscht sie nicht).
     */
    private val keptSilence: Int?
        get() = silence?.takeIf { silenceDefault == null || it != silenceDefault }

    /**
     * Die Anpassung für den Start; Phasenlängen nur, wenn sie von der Vorgabe abweichen. „Danach
     * Stille“ immer, sobald es die Zeile gibt (auch die Vorgabe): So läuft, was die Leiste zeigt.
     */
    fun config(): SessionConfig {
        val base = if (defaults.roundBased) {
            SessionConfig(rounds = duration, silenceMinutes = silenceMinutes)
        } else {
            SessionConfig(minutes = duration, silenceMinutes = silenceMinutes)
        }
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
        silence = null
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
        stored.silence?.let { silence = it.coerceIn(AdjustLimits.SILENCE_MINUTES) }
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
                listOf(it.duration, it.inhale ?: 0, it.hold ?: 0, it.exhale ?: 0, if (it.storedApplied) 1 else 0, it.silence ?: NO_PHASE) +
                    encode(it.defaults)
            },
            restore = {
                if (it.drop(SAVED_VALUES) != encode(defaults)) {
                    SessionAdjustState(defaults)
                } else {
                    SessionAdjustState(defaults, it[0], it[1], it[2], it[3], silence = it[5].takeIf { s -> s != NO_PHASE }, storedApplied = it[4] == 1)
                }
            },
        )

        private const val SAVED_VALUES = 6
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
 *
 * [initialMinutes]: Dauer für diesen Einstieg (Willkommenskarte), nur bei Übungen nach Minuten,
 * geklemmt auf die Grenzen des Steppers. Sie gilt als Abweichung ([SessionAdjustState.modified]):
 * Der Reset führt zur Voreinstellung zurück, eine gespeicherte Anpassung überschreibt sie nicht, und
 * gespeichert wird sie erst, wenn der Nutzer selbst etwas verstellt.
 */
@Composable
fun rememberSessionAdjustState(exercise: Exercise, initialMinutes: Int? = null): SessionAdjustState {
    val defaults = remember(exercise) { AdjustDefaults.of(exercise) }
    return rememberSaveable(exercise.id, defaults, saver = SessionAdjustState.saver(defaults)) {
        val duration = initialMinutes?.takeIf { !defaults.roundBased }?.coerceIn(defaults.durationRange) ?: defaults.duration
        SessionAdjustState(defaults, duration = duration)
    }
}
