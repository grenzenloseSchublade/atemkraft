package app.atemkraft.ui.meditation

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Zustand der Dauerwahl: Klemmen, Übergänge ∞ ↔ Minuten, Rückgabewerte, Saver. */
class DurationPickerStateTest {

    @Test
    fun `Anfangswerte werden auf 1 bis 60 geklemmt`() {
        assertEquals(1, DurationPickerState(0, false).minutes)
        assertEquals(1, DurationPickerState(-5, false).minutes)
        assertEquals(60, DurationPickerState(61, false).minutes)
        assertEquals(60, DurationPickerState(90, false).minutes) // alte 90-min-Einstellung
        assertEquals(20, DurationPickerState(20, true).minutes)
        assertTrue(DurationPickerState(20, true).free)
        assertFalse(DurationPickerState(20, true).fineAdjust)
    }

    @Test
    fun `select klemmt und meldet nur echte Aenderungen`() {
        val state = DurationPickerState(10, false)
        assertFalse(state.select(10))
        assertTrue(state.select(12))
        assertEquals(12, state.minutes)
        assertTrue(state.select(0))
        assertEquals(1, state.minutes)
        assertFalse(state.select(-3)) // bleibt 1
        assertTrue(state.select(90))
        assertEquals(60, state.minutes)
        assertFalse(state.select(61)) // bleibt 60
    }

    @Test
    fun `selectFree behaelt den letzten Minutenwert, select verlaesst offen`() {
        val state = DurationPickerState(15, false)
        state.selectFree()
        assertTrue(state.free)
        assertEquals(15, state.minutes)
        // Gleicher Wert aus ∞ heraus zählt als Änderung (∞ → 15).
        assertTrue(state.select(15))
        assertFalse(state.free)
        assertEquals(15, state.minutes)
    }

    @Test
    fun `step zaehlt in Minuten und haelt an den Grenzen`() {
        val state = DurationPickerState(59, false)
        assertTrue(state.step(1))
        assertEquals(60, state.minutes)
        assertFalse(state.step(1))
        assertEquals(60, state.minutes)

        state.select(2)
        assertTrue(state.step(-1))
        assertEquals(1, state.minutes)
        assertFalse(state.step(-1))
        assertEquals(1, state.minutes)
    }

    @Test
    fun `step aus offen heraus startet beim letzten Minutenwert`() {
        val up = DurationPickerState(20, true)
        assertTrue(up.step(1))
        assertFalse(up.free)
        assertEquals(21, up.minutes)

        val down = DurationPickerState(20, true)
        assertTrue(down.step(-1))
        assertEquals(19, down.minutes)

        // Auch an der Grenze ist das Verlassen von ∞ eine Änderung.
        val atMax = DurationPickerState(60, true)
        assertTrue(atMax.step(1))
        assertFalse(atMax.free)
        assertEquals(60, atMax.minutes)
    }

    @Test
    fun `selectFree meldet nur den Wechsel nach offen`() {
        val state = DurationPickerState(15, false)
        assertTrue(state.selectFree())
        assertFalse(state.selectFree())
    }

    @Test
    fun `Raste-Regeln fuer jede Darstellung`() {
        val ticks = mutableListOf<Boolean>()
        val state = DurationPickerState(13, false)
        state.selectWithTick(16, ticks::add) // 15 überfahren: deutlich
        state.selectWithTick(16, ticks::add) // keine Änderung: keine Raste
        state.stepWithTick(1, ticks::add) // 17: leicht
        state.stepWithTick(-2, ticks::add) // 15 erreicht: deutlich
        state.selectFreeWithTick(ticks::add) // ∞: deutlich
        state.selectFreeWithTick(ticks::add) // schon ∞: keine Raste
        state.stepWithTick(1, ticks::add) // aus ∞ auf 16: leicht
        state.selectFreeWithTick(ticks::add)
        state.selectWithTick(20, ticks::add) // aus ∞ auf 20: deutlich
        assertEquals(listOf(true, false, true, true, false, true, true), ticks)
        assertEquals(20, state.minutes)
        assertFalse(state.free)
    }

    @Test
    fun `Feinwahl schaltet um und ist unabhaengig vom Wert`() {
        val state = DurationPickerState(10, false)
        state.toggleFineAdjust()
        assertTrue(state.fineAdjust)
        state.step(1)
        state.selectFree()
        assertTrue(state.fineAdjust)
        state.toggleFineAdjust()
        assertFalse(state.fineAdjust)
    }

    @Test
    fun `Saver sichert Minuten, offen und Feinwahl`() {
        val state = DurationPickerState(42, false).apply {
            selectFree()
            toggleFineAdjust()
        }
        val restored = roundTrip(state)
        assertEquals(42, restored.minutes)
        assertTrue(restored.free)
        assertTrue(restored.fineAdjust)

        val plain = roundTrip(DurationPickerState(7, false))
        assertEquals(7, plain.minutes)
        assertFalse(plain.free)
        assertFalse(plain.fineAdjust)
    }

    private fun roundTrip(state: DurationPickerState): DurationPickerState {
        @Suppress("UNCHECKED_CAST")
        val saver = DurationPickerState.Saver as Saver<DurationPickerState, Any>
        val scope = SaverScope { true }
        val saved = with(saver) { scope.save(state) }
        assertNotNull(saved)
        return requireNotNull(saver.restore(saved!!))
    }
}
