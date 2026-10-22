package app.atemkraft.ui.patterns

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import app.atemkraft.domain.PatternComposer
import app.atemkraft.domain.PatternField
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.PatternWish
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.domain.withinCreationRules
import app.atemkraft.domain.withinGuardrails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Zustand des Muster-Editors: Startwerte, Wünsche, Klemmen, Name, Lesezeichen, Einpassung, Saver. */
class PatternEditorStateTest {

    private val prefix = "Eigenes Muster"

    private fun spec(inhale: Number, holdFull: Number?, exhale: Number, holdEmpty: Number?, activating: Boolean = false) = PatternSpec(inhale.toDouble(), holdFull?.toDouble(), exhale.toDouble(), holdEmpty?.toDouble(), activating)

    // ---- Startwerte ----

    @Test
    fun `neues Muster startet mit dem typischen Vorschlag ruhig ohne Halten und 5 min`() {
        val state = PatternEditorState.new(prefix)
        assertEquals(spec(4, null, 6, null), state.spec)
        assertEquals(state.spec, state.suggestion)
        assertEquals(PatternWish(activating = false, withHold = false), state.wish)
        assertEquals(0, state.variant)
        assertEquals(5, state.minutes)
        assertFalse(state.modified)
        assertFalse(state.saved)
        assertNull(state.savedId)
        assertFalse(state.existedBefore)
        assertFalse(state.fittedOnOpen)
        assertEquals("Eigenes Muster 4·6", state.name)
        assertEquals(RandomPatternGenerator.DRAFT_ID, state.exercise().id)
    }

    @Test
    fun `aus dem Tagesmuster sind die Ausgangswerte der Vorschlag`() {
        val daily = spec(4, 2, 6, null)
        val state = PatternEditorState.fromDaily(prefix, daily)
        assertEquals(daily, state.spec)
        assertEquals(daily, state.suggestion)
        assertEquals(PatternComposer.wishOf(daily), state.wish)
        assertEquals(PatternComposer.candidates(state.wish).indexOf(daily), state.variant)
        assertFalse(state.modified)
        assertFalse(state.saved)
        assertFalse(state.fittedOnOpen)
        assertEquals("Eigenes Muster 4·2·6", state.name)
    }

    @Test
    fun `Bearbeiten startet gespeichert mit Id, Dauer und Name`() {
        val stored = spec(4, 1, 6, null)
        val state = PatternEditorState.forEdit(prefix, id = 7, spec = stored, minutes = 12, name = "Abendruhe")
        assertEquals(stored, state.spec)
        assertEquals(stored, state.suggestion)
        assertEquals(PatternComposer.wishOf(stored), state.wish)
        assertEquals(12, state.minutes)
        assertEquals(7L, state.savedId)
        assertTrue(state.existedBefore)
        assertTrue(state.saved)
        assertEquals("Abendruhe", state.name)
        assertTrue(state.nameEdited)
        assertEquals("muster-7", state.exercise().id)
        assertEquals("Abendruhe", state.exercise().name)
    }

    @Test
    fun `Bearbeiten ohne gespeicherte Dauer nimmt 5 min und bleibt gespeichert`() {
        val state = PatternEditorState.forEdit(prefix, 3, spec(4, null, 6, null), minutes = null, name = "Tagesmuster 4·6")
        assertEquals(5, state.minutes)
        assertTrue(state.saved)
    }

    @Test
    fun `ein Zahlen-Name bleibt bis zur ersten Aenderung und folgt dann den Zahlen`() {
        val state = PatternEditorState.forEdit(prefix, 3, spec(4, 2, 6, null), 5, "Tagesmuster 4·2·6")
        assertFalse(state.nameEdited)
        assertEquals("Tagesmuster 4·2·6", state.name)
        assertTrue(state.saved)
        state.set(PatternField.HOLD_FULL, 1)
        assertEquals("Eigenes Muster 4·1·6", state.name)
        state.set(PatternField.HOLD_FULL, 2)
        assertEquals("Tagesmuster 4·2·6", state.name)
        assertTrue(state.saved)
    }

    // ---- Wünsche ----

    @Test
    fun `Wunschwechsel erzeugt einen gueltigen passenden Vorschlag`() {
        val state = PatternEditorState.new(prefix)
        state.chooseCharacter(true)
        assertTrue(state.spec.activating)
        assertTrue(state.spec.exhale < state.spec.inhale)
        assertTrue(state.spec.withinCreationRules())
        assertEquals(state.spec, state.suggestion)
        assertEquals(spec(4, null, 3, null, activating = true), state.spec)

        state.chooseHold(true)
        assertEquals(PatternWish(activating = true, withHold = true), state.wish)
        assertEquals(spec(4, 1, 3, null, activating = true), state.spec)

        state.chooseCharacter(false)
        assertFalse(state.spec.activating)
        assertTrue(state.spec.exhale > state.spec.inhale)
        assertEquals(spec(4, 1, 6, null), state.spec)
    }

    @Test
    fun `ohne setzt beide Halten auf null`() {
        val state = PatternEditorState.fromDaily(prefix, spec(4, 2, 6, 1))
        state.chooseHold(false)
        assertNull(state.spec.holdFull)
        assertNull(state.spec.holdEmpty)
        assertFalse(state.wish.withHold)
    }

    @Test
    fun `aktivierend entfernt Halten leer`() {
        val state = PatternEditorState.fromDaily(prefix, spec(4, 1, 6, 2))
        state.chooseCharacter(true)
        assertNull(state.spec.holdEmpty)
        assertNull(state.rangeFor(PatternField.HOLD_EMPTY))
        state.set(PatternField.HOLD_EMPTY, 2)
        assertNull(state.spec.holdEmpty)
    }

    @Test
    fun `gleicher Wunsch erneut gewaehlt behaelt die Feinwerte`() {
        val state = PatternEditorState.new(prefix)
        state.set(PatternField.EXHALE, 7)
        val before = state.spec
        state.chooseCharacter(false)
        state.chooseHold(false)
        assertEquals(before, state.spec)
        assertTrue(state.modified)
    }

    @Test
    fun `nextSuggestion blaettert ohne Wiederholung durch alle passenden Muster`() {
        for (wish in allWishes) {
            val state = PatternEditorState.new(prefix, wish)
            val all = PatternComposer.candidates(wish)
            val seen = mutableListOf(state.spec)
            repeat(all.size - 1) {
                state.nextSuggestion()
                assertEquals(state.spec, state.suggestion)
                seen += state.spec
            }
            assertEquals("$wish", all.size, seen.toSet().size)
            assertEquals(all.toSet(), seen.toSet())
            state.nextSuggestion()
            assertEquals(seen.first(), state.spec)
        }
    }

    @Test
    fun `nextSuggestion nach Samana beginnt die Liste vorn`() {
        val samana = spec(5, 1, 5, null)
        val state = PatternEditorState.fromDaily(prefix, samana)
        assertEquals(-1, state.variant)
        state.nextSuggestion()
        assertEquals(PatternComposer.propose(PatternWish(false, true), 0), state.spec)
        assertEquals(0, state.variant)
    }

    @Test
    fun `Halten per Stepper stellt den Wunsch auf mit und nextSuggestion folgt ihm`() {
        val state = PatternEditorState.new(prefix)
        state.set(PatternField.HOLD_FULL, 1)
        assertTrue(state.wish.withHold)
        state.nextSuggestion()
        assertEquals(PatternComposer.propose(PatternWish(false, true), 0), state.spec)
        state.nextSuggestion()
        assertEquals(PatternComposer.propose(PatternWish(false, true), 1), state.spec)
    }

    // ---- Klemmen und Invariante ----

    @Test
    fun `set ausserhalb der Grenzen wird geklemmt`() {
        val state = PatternEditorState.new(prefix)
        state.set(PatternField.EXHALE, 99)
        assertEquals(state.rangeFor(PatternField.EXHALE)!!.last, state.secondsOf(PatternField.EXHALE))
        state.set(PatternField.INHALE, -5)
        assertEquals(state.rangeFor(PatternField.INHALE)!!.first, state.secondsOf(PatternField.INHALE))
        state.set(PatternField.HOLD_FULL, 50)
        assertEquals(state.rangeFor(PatternField.HOLD_FULL)!!.last, state.secondsOf(PatternField.HOLD_FULL))
        assertTrue(state.spec.withinCreationRules())
    }

    @Test
    fun `kein erreichbarer Zustand verletzt die Erzeugungsregeln`() {
        val random = Random(20261022)
        val starts = listOf(
            PatternEditorState.new(prefix),
            PatternEditorState.fromDaily(prefix, spec(4.5, 1.5, 6.5, null)),
            PatternEditorState.forEdit(prefix, 1, spec(4, 1, 4, null, activating = true), 5, "x"),
        )
        for (state in starts) {
            repeat(3000) {
                when (random.nextInt(6)) {
                    0, 1 -> state.set(PatternField.entries.random(random), random.nextInt(-3, 16))
                    2 -> state.chooseCharacter(random.nextBoolean())
                    3 -> state.chooseHold(random.nextBoolean())
                    4 -> state.nextSuggestion()
                    else -> state.reset()
                }
                assertTrue("${state.spec}", state.spec.withinCreationRules())
                assertTrue("${state.suggestion}", state.suggestion.withinCreationRules())
                for (field in PatternField.entries) {
                    state.rangeFor(field)?.let { assertTrue(state.secondsOf(field) in it) }
                }
            }
        }
    }

    // ---- angepasst, Reset, Minuten ----

    @Test
    fun `modified und reset`() {
        val state = PatternEditorState.new(prefix)
        state.set(PatternField.EXHALE, 7)
        assertTrue(state.modified)
        state.minutes = 12
        state.reset()
        assertFalse(state.modified)
        assertEquals(state.suggestion, state.spec)
        assertEquals(12, state.minutes)
    }

    @Test
    fun `Minuten aendern weder Vorschlag noch Muster und werden geklemmt`() {
        val state = PatternEditorState.new(prefix)
        val spec = state.spec
        val suggestion = state.suggestion
        state.minutes = 20
        assertEquals(spec, state.spec)
        assertEquals(suggestion, state.suggestion)
        assertFalse(state.modified)
        state.minutes = 0
        assertEquals(1, state.minutes)
        state.minutes = 31
        assertEquals(30, state.minutes)
        assertEquals(30, state.config().minutes)
    }

    @Test
    fun `die Uebung hat die eingestellte Dauer`() {
        val state = PatternEditorState.new(prefix)
        for (minutes in listOf(1, 3, 5, 10, 30)) {
            state.minutes = minutes
            assertTrue("$minutes", kotlin.math.abs(state.exercise().defaultMinutes() - minutes) <= 1)
        }
    }

    // ---- Lesezeichen ----

    @Test
    fun `saved wird nach einer Wertaenderung false und markSaved fuellt es wieder`() {
        val state = PatternEditorState.new(prefix)
        state.markSaved(42)
        assertTrue(state.saved)
        assertEquals("muster-42", state.exercise().id)
        state.set(PatternField.EXHALE, 7)
        assertFalse(state.saved)
        state.markSaved(42)
        assertTrue(state.saved)
        state.minutes = 9
        assertFalse(state.saved)
        state.markSaved(42)
        state.updateName("Morgens")
        assertFalse(state.saved)
        state.markSaved(42)
        assertTrue(state.saved)
        assertEquals("Morgens", state.snapshot?.name)
    }

    @Test
    fun `markRemoved leert das Lesezeichen und macht das Muster sofort umkehrbar`() {
        val state = PatternEditorState.forEdit(prefix, 5, spec(4, 1, 6, null), 5, "Eigenes Muster 4·1·6")
        assertTrue(state.existedBefore)
        state.markRemoved()
        assertFalse(state.saved)
        assertNull(state.savedId)
        assertFalse(state.existedBefore)
        assertEquals(RandomPatternGenerator.DRAFT_ID, state.exercise().id)
    }

    // ---- Name ----

    @Test
    fun `Name folgt den Zahlen bis getippt wird`() {
        val state = PatternEditorState.new(prefix)
        state.set(PatternField.EXHALE, 7)
        assertEquals("Eigenes Muster 4·7", state.name)
        state.updateName("Mein Abend")
        state.set(PatternField.EXHALE, 6)
        assertEquals("Mein Abend", state.name)
        assertEquals("Mein Abend", state.savedName)
    }

    @Test
    fun `Name hat hoechstens 80 Zeichen, leer heisst Vorschlagsname`() {
        val state = PatternEditorState.new(prefix)
        state.updateName("a".repeat(100))
        assertEquals(PatternEditorState.NAME_MAX, state.name.length)
        state.updateName("   ")
        assertEquals("   ", state.name)
        assertEquals("Eigenes Muster 4·6", state.savedName)
        assertEquals("Eigenes Muster 4·6", state.exercise().name)
    }

    // ---- Einordnung ----

    @Test
    fun `Einordnung wechselt bei Ein gleich Aus zu Samana`() {
        val state = PatternEditorState.new(prefix, PatternWish(activating = false, withHold = true))
        assertEquals("langhana", state.exercise().lore?.patternId)
        assertEquals(listOf("halten-voll"), state.exercise().lore?.morePatternIds)
        state.set(PatternField.EXHALE, state.secondsOf(PatternField.INHALE))
        assertEquals(state.spec.inhale, state.spec.exhale, 0.0)
        assertEquals("samana", state.exercise().lore?.patternId)
        state.chooseCharacter(true)
        assertEquals("brmhana", state.exercise().lore?.patternId)
        assertTrue(state.exercise().generated)
    }

    // ---- Einpassung alter Muster ----

    @Test
    fun `Muster in den Regeln bleiben beim Oeffnen unveraendert`() {
        for (wish in allWishes) {
            for (candidate in PatternComposer.candidates(wish)) {
                assertEquals(candidate, PatternEditorState.fitToCreationRules(candidate))
            }
        }
    }

    @Test
    fun `alte Muster werden gerundet und auf die Regeln geklemmt`() {
        // Halbe Sekunden: kaufmännisch gerundet.
        assertEquals(spec(5, null, 7, null), PatternEditorState.fitToCreationRules(spec(4.5, null, 6.5, null)))
        // Rundung sprengt den Atemzug (12,5 → 14 s): Ausatmen wird zuerst geklemmt.
        assertEquals(spec(5, 2, 6, null), PatternEditorState.fitToCreationRules(spec(4.5, 1.5, 6.5, null)))
        // Aktivierend mit Ein = Aus: Ausatmen wird kürzer (Brmhana).
        assertEquals(spec(4, null, 3, null, true), PatternEditorState.fitToCreationRules(spec(4, null, 4, null, true)))
        // Ruhiger Atemzug 8,5 s → 9 s.
        assertEquals(spec(4, null, 5, null), PatternEditorState.fitToCreationRules(spec(3.5, null, 5, null)))
        // Rundung sprengt den Atemzug (13 → 15 s): Einatmen wird geklemmt.
        val wide = PatternEditorState.fitToCreationRules(spec(4.5, 1.5, 5.5, 1.5))
        assertTrue("$wide", wide.withinCreationRules())
        // Werte weit außerhalb: typischer Vorschlag zu den Wünschen.
        assertEquals(
            PatternComposer.propose(PatternWish(false, false), 0),
            PatternEditorState.fitToCreationRules(spec(Double.NaN, null, 6, null)),
        )
    }

    @Test
    fun `jedes Muster in den Import-Leitplanken laesst sich einpassen`() {
        val halves = (0..16).map { it / 2.0 }
        var fitted = 0
        for (activating in listOf(false, true)) {
            for (inhale in halves) {
                for (exhale in halves) {
                    for (full in halves.filter { it <= 4 }) {
                        for (empty in halves.filter { it <= 2 }) {
                            val original = spec(inhale, full.takeIf { it > 0 }, exhale, empty.takeIf { it > 0 }, activating)
                            if (!original.withinGuardrails()) continue
                            val result = PatternEditorState.fitToCreationRules(original)
                            assertTrue("$original → $result", result.withinCreationRules())
                            assertEquals(activating, result.activating)
                            if (original.withinCreationRules()) assertEquals(original, result) else fitted++
                            // Höchstens zwei Sekunden je Phase verschoben (sonst wäre es kein Einpassen).
                            PatternField.entries.forEach { field ->
                                val before = original.rangeForValue(field)
                                val after = result.rangeForValue(field)
                                assertTrue("$original → $result ($field)", kotlin.math.abs(after - before) <= 2.0)
                            }
                        }
                    }
                }
            }
        }
        assertTrue(fitted > 0)
    }

    @Test
    fun `eingepasstes Muster beim Bearbeiten startet mit leerem Lesezeichen`() {
        val stored = spec(4.5, null, 6.5, null)
        val state = PatternEditorState.forEdit(prefix, 9, stored, 5, "Tagesmuster 4,5·6,5")
        assertTrue(state.fittedOnOpen)
        assertEquals(spec(5, null, 7, null), state.spec)
        assertFalse(state.modified)
        assertFalse(state.saved)
        assertEquals("Eigenes Muster 5·7", state.name)
        state.markSaved(9)
        assertTrue(state.saved)
    }

    @Test
    fun `eingepasstes Tagesmuster wird uebernommen`() {
        val state = PatternEditorState.fromDaily(prefix, spec(4, 1, 4, null, activating = true))
        assertTrue(state.fittedOnOpen)
        assertTrue(state.spec.withinCreationRules())
        assertTrue(state.spec.activating)
    }

    // ---- Saver ----

    @Test
    fun `Saver sichert den ganzen Zustand`() {
        val state = PatternEditorState.forEdit(prefix, 11, spec(4, 2, 6, null), 8, "Tagesmuster 4·2·6").apply {
            nextSuggestion()
            set(PatternField.EXHALE, secondsOf(PatternField.EXHALE) + 1)
            minutes = 14
        }
        val restored = roundTrip(state)
        assertSameState(state, restored)
        assertEquals("Eigenes Muster", restored.namePrefix)

        val named = PatternEditorState.new(prefix, PatternWish(true, true)).apply {
            updateName("Wach")
            markSaved(3)
            markRemoved()
        }
        assertSameState(named, roundTrip(named))

        val saved = PatternEditorState.forEdit(prefix, 4, spec(4, 1, 6, 1), null, "Eigenes Muster 4·1·6·1")
        val restoredSaved = roundTrip(saved)
        assertTrue(restoredSaved.saved)
        assertEquals("Eigenes Muster 4·1·6·1", restoredSaved.name)
        restoredSaved.set(PatternField.HOLD_EMPTY, 2)
        assertFalse(restoredSaved.saved)
        assertEquals("Eigenes Muster 4·1·6·2", restoredSaved.name)
        assertSameState(saved, roundTrip(saved))

        val fitted = PatternEditorState.forEdit(prefix, 5, spec(4.5, null, 6.5, null), 5, "x")
        assertSameState(fitted, roundTrip(fitted))
    }

    private fun assertSameState(expected: PatternEditorState, actual: PatternEditorState) {
        assertEquals(expected.spec, actual.spec)
        assertEquals(expected.suggestion, actual.suggestion)
        assertEquals(expected.variant, actual.variant)
        assertEquals(expected.minutes, actual.minutes)
        assertEquals(expected.name, actual.name)
        assertEquals(expected.nameEdited, actual.nameEdited)
        assertEquals(expected.savedId, actual.savedId)
        assertEquals(expected.snapshot, actual.snapshot)
        assertEquals(expected.saved, actual.saved)
        assertEquals(expected.existedBefore, actual.existedBefore)
        assertEquals(expected.fittedOnOpen, actual.fittedOnOpen)
        assertEquals(expected.modified, actual.modified)
        assertEquals(expected.exercise(), actual.exercise())
        // Nach dem Wiederherstellen blättert „Anderer Vorschlag“ an derselben Stelle weiter.
        expected.nextSuggestion()
        actual.nextSuggestion()
        assertEquals(expected.spec, actual.spec)
        // Der Name folgt nach dem Wiederherstellen denselben Regeln.
        expected.set(PatternField.EXHALE, expected.rangeFor(PatternField.EXHALE)!!.last)
        actual.set(PatternField.EXHALE, actual.rangeFor(PatternField.EXHALE)!!.last)
        assertEquals(expected.name, actual.name)
    }

    private fun roundTrip(state: PatternEditorState): PatternEditorState {
        @Suppress("UNCHECKED_CAST")
        val saver = PatternEditorState.saver(prefix) as Saver<PatternEditorState, Any>
        val scope = SaverScope { true }
        val saved = with(saver) { scope.save(state) }
        assertNotNull(saved)
        (saved as List<*>).forEach { assertNotNull("Saver-Liste ohne null", it) }
        return requireNotNull(saver.restore(saved))
    }

    private fun PatternSpec.rangeForValue(field: PatternField): Double = when (field) {
        PatternField.INHALE -> inhale
        PatternField.HOLD_FULL -> holdFull ?: 0.0
        PatternField.EXHALE -> exhale
        PatternField.HOLD_EMPTY -> holdEmpty ?: 0.0
    }

    private val allWishes = listOf(false, true).flatMap { a -> listOf(false, true).map { PatternWish(a, it) } }
}
