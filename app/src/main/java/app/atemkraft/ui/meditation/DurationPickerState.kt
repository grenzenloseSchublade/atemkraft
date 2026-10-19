package app.atemkraft.ui.meditation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.atemkraft.domain.MEDITATION_MAX_MINUTES
import app.atemkraft.domain.MEDITATION_MIN_MINUTES

/** Kürzeste wählbare Dauer (Minuten). */
const val DURATION_MIN_MINUTES = MEDITATION_MIN_MINUTES

/** Längste wählbare Dauer (Minuten) – eine Runde des Zeitrings. */
const val DURATION_MAX_MINUTES = MEDITATION_MAX_MINUTES

/**
 * Zustand der Dauerwahl, getrennt von der Darstellung: Zeitring (Variante E) und wischbare
 * Skala (Variante F) lesen und ändern nur diesen Zustand – über die `…WithTick`-Funktionen unten,
 * die zugleich die Haptik-Raste melden.
 *
 * [minutes] bleibt auch bei offener Sitzung ([free]) erhalten, damit der Weg zurück beim
 * letzten Wert landet. Werte außerhalb 1..60 (etwa gespeicherte 90) werden geklemmt.
 */
@Stable
class DurationPickerState(initialMinutes: Int, initialFree: Boolean) {

    /** Gewählte Dauer, 1..60. */
    var minutes: Int by mutableIntStateOf(clampMinutes(initialMinutes))
        private set

    /** Offene Sitzung (∞). */
    var free: Boolean by mutableStateOf(initialFree)
        private set

    /** Feinwahl (− / + neben der Zeit) ist offen. */
    var fineAdjust: Boolean by mutableStateOf(false)
        private set

    /**
     * Setzt die Dauer (geklemmt) und verlässt ∞. True, wenn sich Minuten oder ∞ geändert
     * haben – Anlass für die Haptik-Raste.
     */
    fun select(minutes: Int): Boolean {
        val next = clampMinutes(minutes)
        val changed = next != this.minutes || free
        this.minutes = next
        free = false
        return changed
    }

    /** Wählt die offene Sitzung; [minutes] bleibt als letzter Wert stehen. True, wenn sie neu ist. */
    fun selectFree(): Boolean {
        if (free) return false
        free = true
        return true
    }

    /** ± [delta] Minuten (geklemmt); aus ∞ heraus ab dem letzten Minutenwert. */
    fun step(delta: Int): Boolean = select(minutes + delta)

    /** Öffnet bzw. schließt die Feinwahl. */
    fun toggleFineAdjust() {
        fineAdjust = !fineAdjust
    }

    companion object {
        /** Sichert Minuten, ∞ und Feinwahl über Konfigurationswechsel und Prozesstod. */
        val Saver: Saver<DurationPickerState, *> = listSaver(
            save = { listOf(it.minutes, it.free, it.fineAdjust) },
            restore = {
                DurationPickerState(it[0] as Int, it[1] as Boolean).apply {
                    fineAdjust = it[2] as Boolean
                }
            },
        )
    }
}

/** [DurationPickerState] über rememberSaveable; neue [keys] setzen auf die Anfangswerte zurück. */
@Composable
fun rememberDurationPickerState(
    initialMinutes: Int,
    initialFree: Boolean,
    vararg keys: Any?,
): DurationPickerState = rememberSaveable(*keys, saver = DurationPickerState.Saver) {
    DurationPickerState(initialMinutes, initialFree)
}

/*
 * Regeln für die Haptik-Raste, für jede Darstellung gleich: Jede echte Änderung meldet eine
 * Raste; deutlich (`major`), wenn eine 5er-Marke erreicht oder überfahren wird und beim Wechsel
 * nach ∞. Aus ∞ heraus zählt die Zielminute.
 */

/** Setzt [minutes] (geklemmt) und meldet die Raste. */
fun DurationPickerState.selectWithTick(minutes: Int, onTick: (major: Boolean) -> Unit) = withTick(onTick) { select(minutes) }

/** ± [delta] Minuten (Feinwahl) samt Raste. */
fun DurationPickerState.stepWithTick(delta: Int, onTick: (major: Boolean) -> Unit) = withTick(onTick) { step(delta) }

/** Wählt ∞ und meldet eine deutliche Raste, falls es neu ist. */
fun DurationPickerState.selectFreeWithTick(onTick: (major: Boolean) -> Unit) {
    if (selectFree()) onTick(true)
}

/**
 * ∞-Knopf: wählt ∞, ein erneuter Tipp gibt es wieder frei und kehrt zum letzten Minutenwert
 * zurück. Beides mit Raste.
 */
fun DurationPickerState.toggleFreeWithTick(onTick: (major: Boolean) -> Unit) {
    if (free) selectWithTick(minutes, onTick) else selectFreeWithTick(onTick)
}

private inline fun DurationPickerState.withTick(onTick: (major: Boolean) -> Unit, change: DurationPickerState.() -> Boolean) {
    val wasFree = free
    val previous = minutes
    if (change()) onTick(if (wasFree) DialScale.isMajor(minutes) else crossesMajor(previous, minutes))
}
