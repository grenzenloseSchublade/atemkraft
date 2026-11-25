package app.atemkraft.cue

import app.atemkraft.domain.PhaseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Die Ansage der Phasen darf nie hinter dem Atem herlaufen: kurze Phasen und ein Fortsetzen mitten
 * in der Phase bekommen kein Wort, offene Phasen (Halten bis zum Atemimpuls) immer.
 */
class PhaseWordTest {

    @Test
    fun onlyRealStartsOfLongEnoughPhasesAreSpoken() {
        assertEquals(PhaseWord.INHALE, PhaseWord.at(PhaseType.INHALE, 4_000, open = false, elapsedMs = 0))
        assertEquals(PhaseWord.INHALE_MORE, PhaseWord.at(PhaseType.INHALE_TOP_UP, 2_000, open = false, elapsedMs = 0))
        assertEquals(PhaseWord.HOLD, PhaseWord.at(PhaseType.HOLD_EMPTY, 0, open = true, elapsedMs = 0))
        assertNull("Feueratmung: zu kurz", PhaseWord.at(PhaseType.EXHALE, 500, open = false, elapsedMs = 0))
        assertNull("Fortsetzen nach der Pause", PhaseWord.at(PhaseType.EXHALE, 3_000, open = false, elapsedMs = 1_000))
    }
}
