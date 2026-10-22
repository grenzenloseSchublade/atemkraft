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
    fun `Klemmen auf 1 bis 60 bei Start, select und step, Aenderung nur bei echtem Wechsel`() {
        assertEquals(1, DurationPickerState(0, false).minutes)
        assertEquals(60, DurationPickerState(90, false).minutes) // alte 90-min-Einstellung

        val state = DurationPickerState(10, false)
        assertFalse(state.select(10))
        assertTrue(state.select(90))
        assertEquals(60, state.minutes)
        assertFalse(state.step(1))
        assertTrue(state.select(1))
        assertFalse(state.step(-1))
        assertEquals(1, state.minutes)
        assertTrue(state.step(1))
        assertEquals(2, state.minutes)
    }

    @Test
    fun `∞ und Minuten - letzter Wert bleibt, Verlassen von ∞ ist immer eine Aenderung`() {
        val state = DurationPickerState(15, false)
        assertTrue(state.selectFree())
        assertFalse(state.selectFree())
        assertEquals(15, state.minutes)
        // Gleicher Wert aus ∞ heraus zählt als Änderung (∞ → 15).
        assertTrue(state.select(15))
        assertFalse(state.free)

        // Schritt aus ∞ startet beim letzten Minutenwert, auch an der Grenze.
        val atMax = DurationPickerState(60, true)
        assertTrue(atMax.step(1))
        assertFalse(atMax.free)
        assertEquals(60, atMax.minutes)

        // Erneuter Tipp auf ∞ gibt den letzten Minutenwert frei, beide Male mit deutlicher Raste.
        val toggle = DurationPickerState(20, false)
        val ticks = mutableListOf<Boolean>()
        toggle.toggleFreeWithTick { ticks += it }
        assertTrue(toggle.free)
        toggle.toggleFreeWithTick { ticks += it }
        assertFalse(toggle.free)
        assertEquals(20, toggle.minutes)
        assertEquals(listOf(true, true), ticks)
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

    /** Die Einstellung übersteht eine Drehung. */
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
