package app.atemkraft.data

import app.atemkraft.data.local.SavedPatternEntity
import app.atemkraft.data.local.columnSpec
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.FreePhase
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.domain.FreePhaseType.HOLD
import app.atemkraft.domain.FreePhaseType.IN
import app.atemkraft.domain.FreePhaseType.OUT
import app.atemkraft.domain.PatternOrigin
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.toFreePattern
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eigene Muster als freie Phasenfolge („Frei gestalten“) in der Datenbank: Spalte `phases`,
 * Zusammenfassung in den vier Feldern, Rückfall bei unlesbarer Folge, Import der Folge.
 * Rundlauf, Dubletten und Plausibilität des Backups stehen in `PatternBackupTest`.
 */
class FreePatternStorageTest {

    private fun free(vararg phases: Pair<FreePhaseType, Int>) = FreePattern(phases.map { (type, seconds) -> FreePhase(type, seconds) })

    /** Viloma I: drei Stufen Einatmen mit Pausen, dann Ausatmen. */
    private val viloma = FreePatternTemplates.all.single { it.label == "Viloma I" }.pattern

    /** Zwölf Phasen, die Höchstzahl. */
    private val twelve = free(IN to 3, HOLD to 1, IN to 3, HOLD to 1, OUT to 4, HOLD to 2, IN to 2, IN to 2, OUT to 3, HOLD to 1, OUT to 3, HOLD to 5)

    // ---- Repository ----

    @Test
    fun `freie Folgen mit Stufen und zwoelf Phasen ueberstehen Speichern und Laden`() = runBlocking {
        // Intensiv (Halten voll 12 s): bleibt nach dem Laden „Vorsicht“ – der Sicherheitsdialog greift.
        val intense = free(IN to 4, HOLD to 12, OUT to 6)
        for (pattern in listOf(viloma, twelve, intense)) {
            assertTrue("Testdaten gültig: $pattern", pattern.isValid())
            val dao = FakeSavedPatternDao()
            val repo = SavedPatternsRepository(dao)

            val saved = repo.saveCustom(pattern, minutes = 9, name = "Stufen")

            val row = dao.rows.value.single()
            assertEquals(pattern.toCompact(), row.phases)
            assertEquals(SavedPatternEntity.ORIGIN_CUSTOM, row.origin)
            assertEquals(9, row.minutes)
            assertEquals(pattern.columnSpec(), PatternSpec(row.inhale, row.holdFull, row.exhale, row.holdEmpty, row.activating))

            val exerciseId = "muster-${saved.id}"
            val expected = RandomPatternGenerator.exerciseFrom(pattern, exerciseId, "Stufen", minutes = 9)
            assertEquals(pattern, saved.pattern)
            assertEquals(expected, saved.exercise)
            assertEquals(PatternOrigin.CUSTOM, saved.origin)
            assertEquals(expected, repo.byExerciseId(exerciseId))
            assertEquals(saved, repo.patterns.first().single())
            assertEquals(saved, repo.loadForEdit(saved.id))
            assertEquals(pattern, RandomPatternGenerator.freePatternOf(saved.exercise))
            assertEquals(pattern == intense, repo.byExerciseId(exerciseId)!!.caution)
        }
    }

    @Test
    fun `ungueltige Folge oder Dauer wird nicht gespeichert`() {
        val repo = SavedPatternsRepository(FakeSavedPatternDao())
        // Die Folgenregeln selbst prüft `FreePatternTest`; hier ein Vertreter je Ablehnungsgrund.
        val noExhale = runCatching { runBlocking { repo.saveCustom(free(IN to 4, HOLD to 4), minutes = 5, name = "") } }.exceptionOrNull()
        assertTrue(noExhale is IllegalArgumentException)
        val tooLong = runCatching { runBlocking { repo.saveCustom(viloma, minutes = 31, name = "") } }.exceptionOrNull()
        assertTrue(tooLong is IllegalArgumentException)
    }

    @Test
    fun `beschaedigte Zeilen - unlesbare Folge faellt auf die vier Felder zurueck, unbekannte Herkunft ist eigen`() = runBlocking {
        val dao = FakeSavedPatternDao()
        dao.insert(row(phases = "in:4,atmen:9", origin = SavedPatternEntity.ORIGIN_CUSTOM))
        dao.insert(row(phases = "in:4,hold:3", origin = SavedPatternEntity.ORIGIN_CUSTOM, created = 2_000))
        dao.insert(row(phases = null, origin = "zukunft", created = 2_500))
        dao.insert(row(phases = "in:2,out:2", origin = SavedPatternEntity.ORIGIN_DAILY, created = 3_000))
        val repo = SavedPatternsRepository(dao)

        val saved = repo.patterns.first().sortedBy { it.id }
        val spec = PatternSpec(4.0, null, 6.0, null, false)

        assertEquals(4, saved.size)
        assertEquals(PatternOrigin.CUSTOM, saved[2].origin)
        saved.forEach { pattern ->
            assertEquals(spec, pattern.spec)
            assertEquals(spec.toFreePattern(), pattern.pattern)
            val expected = RandomPatternGenerator.exerciseFrom(spec, "muster-${pattern.id}", "Muster", pattern.origin, null)
            assertEquals(expected, pattern.exercise)
        }
        assertEquals(PatternOrigin.SAVED_DAILY, saved.last().origin)
        // Die beschädigte Folge geht nicht ins Backup, die vier Felder schon.
        assertEquals(listOf(null, null, null, null), repo.exportFile(0) { null }.patterns.map { it.phases })
    }

    @Test
    fun `Zusammenfassung in vier Feldern teilt Halten nach dem Fuellstand auf`() {
        // Viloma I: Pausen bei 1/3 (unten) sowie 2/3 und voll (oben).
        assertEquals(PatternSpec(6.0, 4.0, 6.0, 2.0, false), viloma.columnSpec())
        assertEquals(PatternSpec(4.0, null, 6.0, null, false), FreePattern.DEFAULT.columnSpec())
        assertEquals(PatternSpec(4.0, 4.0, 4.0, 4.0, false), FreePatternTemplates.all.single { it.label == "Box" }.pattern.columnSpec())
        assertEquals(PatternSpec(6.0, null, 3.0, null, true), free(IN to 6, OUT to 3).columnSpec())
    }

    // ---- Backup ----

    @Test
    fun `Import nimmt die Folge, normalisiert sie und rechnet die vier Felder neu`() = runBlocking {
        // Die vier Felder passen nicht zur Folge und lägen außerhalb der Leitplanken: Sie zählen nicht.
        val text = backup(
            """{"name":"M","inhale":99.0,"holdFull":30.0,"exhale":1.0,"activating":true,"createdAtEpochMs":1,
                "phases":" IN : 4 , Hold:30 ,out:6","origin":"zukunft"}""",
        )

        val decoded = PatternBackup.decode(text)!!

        assertEquals(0, decoded.invalid)
        val entry = decoded.entries.single()
        val pattern = free(IN to 4, HOLD to 30, OUT to 6)
        assertEquals(pattern.toCompact(), entry.phases)
        assertEquals(pattern.columnSpec(), PatternSpec(entry.inhale, entry.holdFull, entry.exhale, entry.holdEmpty, entry.activating))

        val dao = FakeSavedPatternDao()
        SavedPatternsRepository(dao).import(decoded.entries) { _, _ -> }
        val row = dao.rows.value.single()
        assertEquals(SavedPatternEntity.ORIGIN_CUSTOM, row.origin)
        assertEquals("in:4,hold:30,out:6", row.phases)
        assertEquals(30.0, row.holdFull!!, 0.0)
        assertEquals(4.0, row.inhale, 0.0)
    }

    private fun row(phases: String?, origin: String, created: Long = 1_000) = SavedPatternEntity(
        name = "Muster",
        inhale = 4.0,
        holdFull = null,
        exhale = 6.0,
        holdEmpty = null,
        activating = false,
        createdAtEpochMs = created,
        origin = origin,
        phases = phases,
    )

    private fun backup(vararg entries: String) = """{"format":"atemkraft-muster","version":1,"exportedAtEpochMs":0,"patterns":[${entries.joinToString(",")}]}"""
}
