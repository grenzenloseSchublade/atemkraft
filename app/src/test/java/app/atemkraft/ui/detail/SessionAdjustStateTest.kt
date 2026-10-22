package app.atemkraft.ui.detail

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.IntervalOverrides
import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.SessionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Anpassung auf der Detailseite ohne Compose: Vorgaben, „angepasst“, Klemmen, einmalige Übernahme. */
class SessionAdjustStateTest {

    private fun exercise(id: String) = BuiltInExercises.all.first { it.id == id }

    private val box = AdjustDefaults.of(exercise("box-4-4-4-4"))

    @Test
    fun `Vorgaben kommen aus den festen Phasen, fehlende Phasen haben keinen Stepper`() {
        assertEquals(AdjustDefaults(roundBased = true, duration = 8, inhale = 4, hold = 4, exhale = 4), box)
        // Wim Hof: Halten leer bis zum Atemreiz ist offen, das feste Halten voll zählt.
        val wimHof = AdjustDefaults.of(exercise("wim-hof"))
        assertEquals(15, wimHof.hold)
        val resonance = AdjustDefaults.of(exercise("resonanz"))
        assertFalse(resonance.roundBased)
        assertNull(resonance.hold)
        assertEquals(AdjustLimits.MINUTES, resonance.durationRange)
    }

    @Test
    fun `nur echte Abweichungen zaehlen - fuer angepasst, Overrides und Start`() {
        val state = SessionAdjustState(box)
        assertFalse(state.modified)
        assertNull(state.overrides)

        state.inhale = 5
        assertTrue(state.intervalsModified)
        assertEquals(IntervalOverrides(duration = 8, inhale = 5, hold = 4, exhale = 4), state.overrides)
        state.inhale = 4
        assertFalse(state.modified)
        assertNull(state.overrides)

        // Start nimmt Phasenlängen nur mit, wenn sie abweichen.
        state.duration = 3
        assertEquals(SessionConfig(rounds = 3), state.config())
        state.exhale = 6
        assertEquals(SessionConfig(rounds = 3, inhaleSeconds = 4.0, exhaleSeconds = 6.0, holdSeconds = 4.0), state.config())
        state.reset()
        assertEquals(SessionConfig(rounds = 8), state.config())
    }

    /** Persistierte Werte: einmal übernommen, geklemmt, die Wahl des Nutzers geht vor, fehlende Phase ohne Wirkung. */
    @Test
    fun `gespeicherte Werte werden einmal übernommen und auf die Grenzen geklemmt`() {
        val state = SessionAdjustState(box)
        state.applyStored(IntervalOverrides(duration = 99, inhale = 1, hold = 50, exhale = 6))
        assertEquals(AdjustLimits.ROUNDS.last, state.duration)
        assertEquals(AdjustLimits.INHALE_S.first, state.inhale)
        assertEquals(AdjustLimits.HOLD_S.last, state.hold)
        assertEquals(6, state.exhale)

        // Spätere Werte (das eigene Zurückschreiben) überschreiben nichts mehr.
        state.exhale = 7
        state.applyStored(IntervalOverrides(duration = 2, exhale = 3))
        assertEquals(7, state.exhale)
        assertEquals(AdjustLimits.ROUNDS.last, state.duration)

        // Hat der Nutzer vorher verstellt, bleibt seine Wahl.
        val chosen = SessionAdjustState(box)
        chosen.duration = 5
        chosen.applyStored(IntervalOverrides(duration = 2, inhale = 6))
        assertEquals(5, chosen.duration)
        assertEquals(4, chosen.inhale)
        assertTrue(chosen.storedApplied)

        // Eine gespeicherte Phase, die die Übung nicht hat, bleibt ohne Wirkung.
        val resonance = SessionAdjustState(AdjustDefaults.of(exercise("resonanz")))
        resonance.applyStored(IntervalOverrides(hold = 10))
        assertNull(resonance.hold)
        assertFalse(resonance.modified)
    }

    private fun saveAndRestore(state: SessionAdjustState, restoreWith: AdjustDefaults): SessionAdjustState {
        val saver = SessionAdjustState.saver(state.defaults)

        @Suppress("UNCHECKED_CAST")
        val saved = with(saver as Saver<SessionAdjustState, Any>) { SaverScope { true }.save(state) }!!
        return (SessionAdjustState.saver(restoreWith) as Saver<SessionAdjustState, Any>).restore(saved)!!
    }

    @Test
    fun `gesicherter Zustand gilt nur fuer dieselben Vorgaben`() {
        val calm = AdjustDefaults(roundBased = false, duration = 5, inhale = 4, hold = null, exhale = 6)
        val state = SessionAdjustState(calm)
        state.duration = 7
        state.applyStored(IntervalOverrides())

        // Gleiche Vorgaben (Drehen, Zurück ohne Änderung): Werte und Merker bleiben.
        val same = saveAndRestore(state, calm)
        assertEquals(7, same.duration)
        assertTrue(same.storedApplied)

        // Im Editor geändert (gleiche Id, neue Vorgaben): frisch bei den neuen Vorgaben, ohne
        // alte Phasen als Abweichung – Start nimmt keine Phasen-Overrides mit.
        val edited = AdjustDefaults(roundBased = false, duration = 10, inhale = 5, hold = 1, exhale = 3)
        val restored = saveAndRestore(state, edited)
        assertFalse(restored.modified)
        assertFalse(restored.storedApplied)
        assertEquals(SessionConfig(minutes = 10), restored.config())
    }
}
