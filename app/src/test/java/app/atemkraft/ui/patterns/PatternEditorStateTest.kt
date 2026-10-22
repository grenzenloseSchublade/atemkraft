package app.atemkraft.ui.patterns

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.FreePhase
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.domain.FreePhaseType.HOLD
import app.atemkraft.domain.FreePhaseType.IN
import app.atemkraft.domain.FreePhaseType.OUT
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.ui.PatternEditorRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Zustand des Muster-Editors (Nachtrag 4 „Frei gestalten“): Startwerte, Vorlagen, Auswahl,
 * Hinzufügen hinter der Auswahl, Entfernen-Grenzen, 12er-Grenze, Intensität, Name, Lesezeichen,
 * Saver.
 */
class PatternEditorStateTest {

    private val prefix = "Eigenes Muster"

    private fun spec(inhale: Number, holdFull: Number?, exhale: Number, holdEmpty: Number?, activating: Boolean = false) = PatternSpec(inhale.toDouble(), holdFull?.toDouble(), exhale.toDouble(), holdEmpty?.toDouble(), activating)

    private fun free(vararg phases: Pair<FreePhaseType, Int>) = FreePattern(phases.map { (type, seconds) -> FreePhase(type, seconds) })

    private fun template(label: String) = FreePatternTemplates.all.single { it.label == label }

    // ---- Startwerte ----

    @Test
    fun `neues Muster startet mit 4·6, letzte Phase gewaehlt und 5 min`() {
        val state = PatternEditorState.new(prefix)
        assertEquals(free(IN to 4, OUT to 6), state.pattern)
        assertEquals("4·6", state.template?.label)
        assertEquals(1, state.selected)
        assertEquals(5, state.minutes)
        assertEquals("Eigenes Muster 4·6", state.name)
        assertFalse(state.modified)
        assertFalse(state.saved)
        assertNull(state.savedId)
    }

    @Test
    fun `das Muster des Tages wird zur freien Folge`() {
        val state = PatternEditorState.fromDaily(prefix, spec(4, 2, 6, 1))
        assertEquals(free(IN to 4, HOLD to 2, OUT to 6, HOLD to 1), state.pattern)
        assertEquals(state.pattern, state.initial)
        assertNull(state.template)
        assertNull(state.savedId)
        // Ältere Werte mit halben Sekunden werden gerundet, sanft aktivierend ohne Halten leer.
        val active = PatternEditorState.fromDaily(prefix, spec(4.5, null, 3, null, activating = true))
        assertEquals(free(IN to 5, OUT to 3), active.pattern)
    }

    @Test
    fun `Bearbeiten startet gespeichert mit Id, Dauer und Name`() {
        val stored = free(IN to 3, IN to 1, OUT to 6)
        val state = PatternEditorState.forEdit(prefix, id = 7, pattern = stored, minutes = 12, name = "Abendruhe")
        assertEquals(stored, state.pattern)
        assertEquals("Seufzer", state.template?.label)
        assertEquals(7L, state.savedId)
        assertEquals(12, state.minutes)
        assertEquals("Abendruhe", state.name)
        assertTrue(state.saved)
        assertTrue(state.existedBefore)
        assertEquals("muster-7", state.exercise().id)
        // Auch beim Bearbeiten ist die letzte Phase gewählt.
        assertEquals(2, state.selected)
    }

    @Test
    fun `Bearbeiten ohne gespeicherte Dauer nimmt 5 min und bleibt gespeichert`() {
        val state = PatternEditorState.forEdit(prefix, id = 3, pattern = FreePattern.DEFAULT, minutes = null, name = "x")
        assertEquals(5, state.minutes)
        assertTrue(state.saved)
    }

    @Test
    fun `ein Zahlen-Name bleibt bis zur ersten Aenderung und folgt dann den Zahlen`() {
        val state = PatternEditorState.forEdit(prefix, 4, free(IN to 4, HOLD to 2, OUT to 6), 5, "Tagesmuster 4·2·6")
        assertEquals("Tagesmuster 4·2·6", state.name)
        assertFalse(state.nameEdited)
        state.setSeconds(2, 7)
        assertEquals("Eigenes Muster 4·2·7", state.name)
        state.setSeconds(2, 6)
        assertEquals("Tagesmuster 4·2·6", state.name)
    }

    // ---- Auswahl, Sekunden ----

    @Test
    fun `es ist immer eine Phase gewaehlt, ausserhalb der Folge bleibt die Auswahl`() {
        val state = PatternEditorState.new(prefix)
        state.select(0)
        assertEquals(0, state.selected)
        // Dieselbe noch einmal hebt die Auswahl nicht auf (Nachtrag 5).
        state.select(0)
        assertEquals(0, state.selected)
        state.select(9)
        assertEquals(0, state.selected)
        state.select(-1)
        assertEquals(0, state.selected)
    }

    @Test
    fun `Sekunden werden auf 1 bis 30 geklemmt`() {
        val state = PatternEditorState.new(prefix)
        state.setSeconds(0, 0)
        assertEquals(1, state.pattern.phases[0].seconds)
        state.setSeconds(1, 99)
        assertEquals(30, state.pattern.phases[1].seconds)
        assertTrue(state.modified)
        assertNull(state.template)
    }

    // ---- Hinzufügen und Entfernen ----

    @Test
    fun `Hinzufuegen kommt hinter die Auswahl, die neue ist gewaehlt`() {
        val state = PatternEditorState.new(prefix)
        state.select(0)
        state.add(HOLD)
        assertEquals(free(IN to 4, HOLD to 2, OUT to 6), state.pattern)
        assertEquals(1, state.selected)

        state.select(2)
        state.add(IN)
        assertEquals(free(IN to 4, HOLD to 2, OUT to 6, IN to 4), state.pattern)
        assertEquals(3, state.selected)

        state.add(OUT)
        assertEquals(free(IN to 4, HOLD to 2, OUT to 6, IN to 4, OUT to 4), state.pattern)
        assertEquals(4, state.selected)
    }

    @Test
    fun `bei 12 Phasen ist Schluss`() {
        val state = PatternEditorState.new(prefix)
        repeat(20) { state.add(HOLD) }
        assertEquals(FreePattern.MAX_PHASES, state.pattern.phases.size)
        assertFalse(state.canAdd)
        val before = state.pattern
        state.add(IN)
        assertEquals(before, state.pattern)
        assertTrue(state.pattern.isValid())
    }

    @Test
    fun `Entfernen nur, wenn die Folge gueltig bleibt, danach ist die Nachbarphase gewaehlt`() {
        val state = PatternEditorState.new(prefix)
        // 4·6: zwei Phasen, das einzige Ein und das einzige Aus – nichts darf weg.
        assertFalse(state.canRemove(0))
        assertFalse(state.canRemove(1))
        state.remove(0)
        assertEquals(FreePattern.DEFAULT, state.pattern)

        state.applyTemplate(template("Box"))
        assertTrue(state.canRemove(1))
        assertFalse(state.canRemove(0))
        state.remove(1)
        assertEquals(free(IN to 4, OUT to 4, HOLD to 4), state.pattern)
        assertEquals(1, state.selected)
        // Die letzte Phase entfernt: gewählt ist die neue letzte.
        state.remove(2)
        assertEquals(free(IN to 4, OUT to 4), state.pattern)
        assertEquals(1, state.selected)
    }

    @Test
    fun `kein erreichbarer Zustand ist ungueltig`() {
        val random = Random(7)
        val state = PatternEditorState.new(prefix)
        repeat(2000) {
            val size = state.pattern.phases.size
            when (random.nextInt(6)) {
                0 -> state.add(FreePhaseType.entries[random.nextInt(3)])
                1 -> state.remove(random.nextInt(size))
                2 -> state.setSeconds(random.nextInt(size), random.nextInt(-5, 40))
                3 -> state.select(random.nextInt(-1, size + 1))
                4 -> state.applyTemplate(FreePatternTemplates.all[random.nextInt(FreePatternTemplates.all.size)])
                else -> state.reset()
            }
            assertTrue("${state.pattern}", state.pattern.isValid())
            assertTrue(state.selected in state.pattern.phases.indices)
        }
    }

    // ---- Vorlagen, Reset ----

    @Test
    fun `Vorlage setzt die Folge und waehlt ihre letzte Phase, Minuten bleiben`() {
        val state = PatternEditorState.new(prefix, minutes = 9)
        state.select(0)
        state.applyTemplate(template("4-7-8"))
        assertEquals(free(IN to 4, HOLD to 7, OUT to 8), state.pattern)
        assertEquals("4-7-8", state.template?.label)
        assertEquals(2, state.selected)
        assertEquals(9, state.minutes)
        assertEquals("Eigenes Muster 4·7·8", state.name)
        // Eine Änderung macht daraus „angepasst“.
        state.setSeconds(0, 5)
        assertNull(state.template)
    }

    @Test
    fun `Reset setzt die Folge auf den Stand beim Oeffnen, Minuten bleiben`() {
        val state = PatternEditorState.new(prefix)
        state.add(HOLD)
        state.minutes = 12
        assertTrue(state.modified)
        state.reset()
        assertEquals(FreePattern.DEFAULT, state.pattern)
        assertFalse(state.modified)
        assertEquals(1, state.selected)
        assertEquals(12, state.minutes)
    }

    @Test
    fun `Minuten aendern die Folge nicht und werden geklemmt`() {
        val state = PatternEditorState.new(prefix)
        state.minutes = 0
        assertEquals(1, state.minutes)
        state.minutes = 99
        assertEquals(30, state.minutes)
        assertEquals(FreePattern.DEFAULT, state.pattern)
        assertTrue(kotlin.math.abs(state.exercise().defaultMinutes() - 30) <= 1)
    }

    // ---- Intensität ----

    @Test
    fun `langes Halten voll macht das Muster intensiv, die Uebung fragt dann nach`() {
        val state = PatternEditorState.new(prefix, template("4-7-8").pattern)
        assertFalse(state.pattern.isIntense)
        assertFalse(state.exercise().caution)
        state.setSeconds(1, 11)
        assertTrue(state.pattern.isIntense)
        assertTrue(state.exercise().caution)
        assertTrue(state.exercise().cautions.isNotEmpty())
        assertNotNull(state.pattern.intensityNote())
    }

    @Test
    fun `schnelles Atmen ueber 20 pro Minute ist intensiv`() {
        val state = PatternEditorState.new(prefix)
        state.setSeconds(0, 1)
        state.setSeconds(1, 1)
        assertTrue(state.pattern.isIntense)
        assertTrue(state.exercise().caution)
    }

    // ---- Name ----

    @Test
    fun `Name folgt den Zahlen bis getippt wird`() {
        val state = PatternEditorState.new(prefix)
        state.add(HOLD)
        assertEquals("Eigenes Muster 4·6·2", state.name)
        state.updateName("Abendruhe")
        state.setSeconds(0, 5)
        assertEquals("Abendruhe", state.name)
        assertTrue(state.nameEdited)
    }

    @Test
    fun `Name hat hoechstens 80 Zeichen, leer heisst Vorschlagsname`() {
        val state = PatternEditorState.new(prefix)
        state.updateName("x".repeat(120))
        assertEquals(PatternEditorState.NAME_MAX, state.name.length)
        state.updateName("   ")
        assertEquals("Eigenes Muster 4·6", state.savedName)
        assertEquals("Eigenes Muster 4·6", state.exercise().name)
    }

    // ---- Lesezeichen ----

    @Test
    fun `saved wird nach einer Aenderung false und markSaved fuellt es wieder`() {
        val state = PatternEditorState.new(prefix)
        state.markSaved(5)
        assertTrue(state.saved)
        state.add(OUT)
        assertFalse(state.saved)
        state.markSaved(5)
        assertTrue(state.saved)
        state.minutes = 6
        assertFalse(state.saved)
    }

    @Test
    fun `markRemoved leert das Lesezeichen und macht das Muster sofort umkehrbar`() {
        val state = PatternEditorState.forEdit(prefix, 7, FreePattern.DEFAULT, 5, "x")
        state.markRemoved()
        assertNull(state.savedId)
        assertNull(state.snapshot)
        assertFalse(state.saved)
        assertFalse(state.existedBefore)
    }

    @Test
    fun `Lesezeichen merkt sich den geschriebenen Stand, nicht den nach dem Schreiben`() {
        val state = PatternEditorState.new(prefix)
        // Stand vor dem Schreiben festhalten, während des Schreibens zieht die Kurve weiter.
        val written = state.draft()
        state.setSeconds(1, 7)
        state.markSaved(42, written)
        assertEquals(42L, state.savedId)
        assertFalse(state.saved)
        // Zurück auf den geschriebenen Stand: Lesezeichen wieder gefüllt.
        state.setSeconds(1, 6)
        assertTrue(state.saved)
    }

    // ---- Saver ----

    @Test
    fun `Saver sichert den ganzen Zustand`() {
        val edited = PatternEditorState.forEdit(prefix, 11, free(IN to 4, HOLD to 2, OUT to 6), 8, "Tagesmuster 4·2·6").apply {
            select(1)
            add(HOLD)
            minutes = 14
        }
        val restored = roundTrip(edited)
        assertSameState(edited, restored)
        assertEquals("Eigenes Muster", restored.namePrefix)

        val named = PatternEditorState.new(prefix, template("Viloma I").pattern).apply {
            updateName("Wach")
            markSaved(3)
            markRemoved()
        }
        assertSameState(named, roundTrip(named))

        val saved = PatternEditorState.forEdit(prefix, 4, free(IN to 4, HOLD to 1, OUT to 6, HOLD to 1), null, "Eigenes Muster 4·1·6·1")
        val restoredSaved = roundTrip(saved)
        assertTrue(restoredSaved.saved)
        assertEquals("Eigenes Muster 4·1·6·1", restoredSaved.name)
        restoredSaved.setSeconds(3, 2)
        assertFalse(restoredSaved.saved)
        assertEquals("Eigenes Muster 4·1·6·2", restoredSaved.name)
    }

    private fun assertSameState(expected: PatternEditorState, actual: PatternEditorState) {
        assertEquals(expected.pattern, actual.pattern)
        assertEquals(expected.initial, actual.initial)
        assertEquals(expected.selected, actual.selected)
        assertEquals(expected.minutes, actual.minutes)
        assertEquals(expected.name, actual.name)
        assertEquals(expected.nameEdited, actual.nameEdited)
        assertEquals(expected.savedId, actual.savedId)
        assertEquals(expected.snapshot, actual.snapshot)
        assertEquals(expected.saved, actual.saved)
        assertEquals(expected.existedBefore, actual.existedBefore)
        assertEquals(expected.modified, actual.modified)
        assertEquals(expected.exercise(), actual.exercise())
        // Nach dem Wiederherstellen gelten dieselben Regeln: Hinzufügen hinter der Auswahl, Name.
        expected.add(OUT)
        actual.add(OUT)
        assertEquals(expected.pattern, actual.pattern)
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

    @Test
    fun `Stift am Tagesmuster gibt genau dessen Zahlen mit`() {
        val shown = RandomPatternGenerator.forDate(java.time.LocalDate.of(2026, 10, 21))
        assertEquals(shown.spec, RandomPatternGenerator.specOf(shown.exercise))
        val route = PatternEditorRoute.of(shown.spec)
        assertTrue(route.fromDaily)
        assertEquals(shown.spec, route.dailySpec())
        // Mit Halten leer und ohne Halten voll, sanft aktivierend.
        listOf(spec(4, null, 6, 2), spec(5, 1, 3, null, activating = true)).forEach {
            assertEquals(it, PatternEditorRoute.of(it).dailySpec())
            assertEquals(it, RandomPatternGenerator.specOf(RandomPatternGenerator.exerciseFrom(it, "x", "x")))
        }
        // Alte Route ohne Zahlen bzw. unlesbare: kein Spec, der Editor nimmt das aktuelle Tagesmuster.
        assertNull(PatternEditorRoute(fromDaily = true).dailySpec())
        assertNull(PatternEditorRoute(fromDaily = true, daily = "4,x,6,0,false").dailySpec())
    }
}
