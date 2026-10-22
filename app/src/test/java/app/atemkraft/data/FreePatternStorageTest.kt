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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Eigene Muster als freie Phasenfolge („Frei gestalten“) in Datenbank und Backup: Spalte
 * `phases`, Zusammenfassung in den vier Feldern, Rückfall bei unlesbarer Folge, Import-Prüfung.
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
        for (pattern in listOf(viloma, twelve, FreePattern.DEFAULT)) {
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
        }
    }

    @Test
    fun `leerer Name wird zum Vorschlag aus den Sekunden der Folge`() = runBlocking {
        val repo = SavedPatternsRepository(FakeSavedPatternDao())

        val saved = repo.saveCustom(free(IN to 3, IN to 1, OUT to 6), minutes = 5, name = "  ")

        assertEquals("Eigenes Muster 3·1·6", saved.exercise.name)
        assertEquals("Eigenes Muster 3·1·6", SavedPatternsRepository.suggestedName(saved.pattern))
    }

    @Test
    fun `ungueltige Folge oder Dauer wird nicht gespeichert`() {
        val repo = SavedPatternsRepository(FakeSavedPatternDao())
        val invalid = listOf(
            free(IN to 4),
            free(IN to 4, HOLD to 4),
            free(IN to 0, OUT to 4),
            free(IN to 31, OUT to 4),
            FreePattern(List(13) { FreePhase(if (it % 2 == 0) IN else OUT, 2) }),
        )
        for (pattern in invalid) {
            val failure = runCatching { runBlocking { repo.saveCustom(pattern, minutes = 5, name = "") } }.exceptionOrNull()
            assertTrue("$pattern", failure is IllegalArgumentException)
        }
        val tooLong = runCatching { runBlocking { repo.saveCustom(viloma, minutes = 31, name = "") } }.exceptionOrNull()
        assertTrue(tooLong is IllegalArgumentException)
    }

    @Test
    fun `update macht aus einem Tagesmuster ein freies, die Variante mit vier Feldern entfernt die Folge`() = runBlocking {
        val daily = RandomPatternGenerator.forDate(LocalDate.of(2026, 10, 5))
        val dao = FakeSavedPatternDao()
        val cleared = mutableListOf<String>()
        val repo = SavedPatternsRepository(dao, clearOverrides = { cleared += it })
        repo.save(daily)
        val id = dao.rows.value.single().id
        val before = dao.rows.value.single()
        assertEquals(daily.spec.toFreePattern(), repo.loadForEdit(id)!!.pattern)

        val updated = repo.update(id, viloma, minutes = 12, name = "Stufen")!!

        val row = dao.rows.value.single()
        assertEquals(viloma.toCompact(), row.phases)
        assertEquals(SavedPatternEntity.ORIGIN_CUSTOM, row.origin)
        assertEquals(before.createdAtEpochMs, row.createdAtEpochMs)
        assertEquals(viloma, updated.pattern)
        assertEquals(RandomPatternGenerator.exerciseFrom(viloma, "muster-$id", "Stufen", 12), updated.exercise)
        assertEquals(listOf("muster-$id"), cleared)
        assertNull(repo.update(999, viloma, minutes = 5, name = ""))
    }

    @Test
    fun `unlesbare Folge faellt auf die vier Felder zurueck, bei Tagesmustern zaehlt sie nicht`() = runBlocking {
        val dao = FakeSavedPatternDao()
        dao.insert(row(phases = "in:4,atmen:9", origin = SavedPatternEntity.ORIGIN_CUSTOM))
        dao.insert(row(phases = "in:4,hold:3", origin = SavedPatternEntity.ORIGIN_CUSTOM, created = 2_000))
        dao.insert(row(phases = "in:2,out:2", origin = SavedPatternEntity.ORIGIN_DAILY, created = 3_000))
        val repo = SavedPatternsRepository(dao)

        val saved = repo.patterns.first().sortedBy { it.id }
        val spec = PatternSpec(4.0, null, 6.0, null, false)

        assertEquals(3, saved.size)
        saved.forEach { pattern ->
            assertEquals(spec, pattern.spec)
            assertEquals(spec.toFreePattern(), pattern.pattern)
            val expected = RandomPatternGenerator.exerciseFrom(spec, "muster-${pattern.id}", "Muster", pattern.origin, null)
            assertEquals(expected, pattern.exercise)
        }
        assertEquals(PatternOrigin.SAVED_DAILY, saved.last().origin)
        // Die beschädigte Folge geht nicht ins Backup, die vier Felder schon.
        assertEquals(listOf(null, null, null), repo.exportFile(0) { null }.patterns.map { it.phases })
    }

    @Test
    fun `ein intensives freies Muster bleibt nach dem Laden caution`() = runBlocking {
        val intense = free(IN to 4, HOLD to 12, OUT to 6)
        val repo = SavedPatternsRepository(FakeSavedPatternDao())

        val saved = repo.saveCustom(intense, minutes = 5, name = "")

        assertTrue(saved.exercise.caution)
        assertTrue(repo.byExerciseId("muster-${saved.id}")!!.caution)
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
    fun `freie Folgen ueberstehen Export und Import, Version bleibt 1`() = runBlocking {
        val source = FakeSavedPatternDao()
        val sourceRepo = SavedPatternsRepository(source)
        sourceRepo.save(RandomPatternGenerator.forDate(LocalDate.of(2026, 10, 5)))
        sourceRepo.saveCustom(viloma, minutes = 7, name = "Stufen")
        sourceRepo.saveCustom(twelve, minutes = 20, name = "Zwölf")
        // Ein älteres eigenes Muster nur mit vier Feldern (Editor vor „Frei gestalten“).
        source.insert(
            SavedPatternEntity(
                name = "Vier Felder",
                inhale = 4.0,
                holdFull = 1.0,
                exhale = 6.0,
                holdEmpty = null,
                activating = false,
                createdAtEpochMs = System.currentTimeMillis(),
                origin = SavedPatternEntity.ORIGIN_CUSTOM,
                minutes = 5,
            ),
        )
        val file = sourceRepo.exportFile(nowEpochMs = 3_000) { null }
        val text = PatternBackup.encode(file)

        assertEquals(1, file.version)
        assertTrue(text.contains("\"phases\": \"${viloma.toCompact()}\""))
        assertEquals(listOf(null, viloma.toCompact(), twelve.toCompact(), null), file.patterns.map { it.phases })

        val decoded = PatternBackup.decode(text)!!
        assertEquals(0, decoded.invalid)
        val target = FakeSavedPatternDao()
        val targetRepo = SavedPatternsRepository(target)
        assertEquals(ImportResult(added = 4, duplicates = 0), targetRepo.import(decoded.entries) { _, _ -> })

        assertEquals(source.rows.value.map { it.copy(id = 0) }, target.rows.value.map { it.copy(id = 0) })
        val imported = targetRepo.patterns.first().filter { it.exercise.name in setOf("Stufen", "Zwölf") }.map { it.pattern }.toSet()
        assertEquals(setOf(viloma, twelve), imported)
        assertEquals(ImportResult(added = 0, duplicates = 4), targetRepo.import(decoded.entries) { _, _ -> })
    }

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

    @Test
    fun `unplausible Folgen im Import fallen weg`() {
        val bad = listOf(
            """"phases":"in:4","origin":"custom"""",
            """"phases":"in:4,hold:3","origin":"custom"""",
            """"phases":"hold:3,out:4","origin":"custom"""",
            """"phases":"in:0,out:4","origin":"custom"""",
            """"phases":"in:31,out:4","origin":"custom"""",
            """"phases":"in:4.5,out:4","origin":"custom"""",
            """"phases":"in:4,out:-4","origin":"custom"""",
            """"phases":"in:4,atmen:4,out:4","origin":"custom"""",
            """"phases":"${List(13) { if (it % 2 == 0) "in:2" else "out:2" }.joinToString(",")}","origin":"custom"""",
            """"phases":"","origin":"custom"""",
            // Folge ohne Herkunft oder bei einem Tagesmuster schreibt die App nie.
            """"phases":"in:4,out:6"""",
            """"phases":"in:4,out:6","origin":"daily"""",
        )
        val good = entry(""""phases":"in:4,out:6","origin":"custom"""")
        val text = backup(*(bad.map { entry(it) } + good).toTypedArray())

        val decoded = PatternBackup.decode(text)!!

        assertEquals(bad.size, decoded.invalid)
        assertEquals(listOf("in:4,out:6"), decoded.entries.map { it.phases })
    }

    @Test
    fun `Dubletten freier Folgen zaehlen nach Zeitpunkt und Folge`() = runBlocking {
        val dao = FakeSavedPatternDao()
        val repo = SavedPatternsRepository(dao)
        repo.saveCustom(viloma, minutes = 5, name = "Stufen")
        val stored = repo.exportFile(0) { null }.patterns.single()
        val sameSpaced = stored.copy(phases = stored.phases!!.replace(",", " , ").uppercase())
        val otherSequence = stored.copy(phases = "in:6,hold:6,out:6")
        val text = PatternBackup.encode(PatternBackupFile(exportedAtEpochMs = 0, patterns = listOf(sameSpaced, otherSequence)))

        val result = repo.import(PatternBackup.decode(text)!!.entries) { _, _ -> }

        assertEquals(ImportResult(added = 1, duplicates = 1), result)
        assertEquals(setOf(viloma.toCompact(), "in:6,hold:6,out:6"), dao.rows.value.map { it.phases }.toSet())
        assertFalse(dao.rows.value.any { it.origin == SavedPatternEntity.ORIGIN_DAILY })
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

    private fun entry(fields: String) = """{"name":"M","inhale":4.0,"exhale":6.0,"activating":false,"createdAtEpochMs":1,$fields}"""

    private fun backup(vararg entries: String) = """{"format":"atemkraft-muster","version":1,"exportedAtEpochMs":0,"patterns":[${entries.joinToString(",")}]}"""
}
