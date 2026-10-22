package app.atemkraft.data

import app.atemkraft.data.local.SavedPatternEntity
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.PatternOrigin
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.toFreePattern
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** SEC-STORE-01/-02: Export/Import gespeicherter Muster – Round-Trip und Import als nicht vertrauenswürdige Eingabe. */
class PatternBackupTest {

    private fun entity(created: Long, origin: String = SavedPatternEntity.ORIGIN_DAILY, minutes: Int? = null) = SavedPatternEntity(
        name = "Muster $created",
        inhale = 4.0,
        holdFull = 2.0,
        exhale = 6.0,
        holdEmpty = null,
        activating = false,
        createdAtEpochMs = created,
        origin = origin,
        minutes = minutes,
    )

    /** Viloma I: drei Stufen Einatmen mit Pausen, dann Ausatmen. */
    private val viloma = FreePatternTemplates.all.single { it.label == "Viloma I" }.pattern

    /** Zwölf Phasen, die Höchstzahl. */
    private val twelve = FreePattern.parse("in:3,hold:1,in:3,hold:1,out:4,hold:2,in:2,in:2,out:3,hold:1,out:3,hold:5")!!

    private fun entry(fields: String, created: Int = 1) = """{"name":"M","inhale":4.0,"exhale":6.0,"activating":false,"createdAtEpochMs":$created${if (fields.isEmpty()) "" else ",$fields"}}"""

    private fun backup(vararg entries: String) = """{"format":"atemkraft-muster","version":1,"exportedAtEpochMs":0,"zukunft":true,"patterns":[${entries.joinToString(",")}]}"""

    /** Ein Rundlauf für alle Mustertypen: Tagesmuster, eigene mit vier Feldern und freie Folgen. */
    @Test
    fun `Export und Import ergeben dieselben Werte und Anpassungen`() = runBlocking {
        val source = FakeSavedPatternDao()
        val sourceRepo = SavedPatternsRepository(source)
        sourceRepo.save(RandomPatternGenerator.forDate(LocalDate.of(2026, 10, 5)))
        val abend = PatternSpec(4.0, 1.0, 6.0, null, false).toFreePattern()
        sourceRepo.saveCustom(abend, minutes = 12, name = "Abend")
        sourceRepo.saveCustom(viloma, minutes = 7, name = "Stufen")
        sourceRepo.saveCustom(twelve, minutes = 20, name = "Zwölf")
        // Ein älteres eigenes Muster nur mit vier Feldern (Editor vor „Frei gestalten“).
        source.insert(entity(4_000, origin = SavedPatternEntity.ORIGIN_CUSTOM, minutes = 5))
        val overrides = mapOf("muster-1" to IntervalOverrides(duration = 7, inhale = 5))
        val file = sourceRepo.exportFile(nowEpochMs = 3_000) { overrides[it] }
        val text = PatternBackup.encode(file)

        assertEquals(1, file.version)
        assertTrue(text.contains("\"origin\": \"custom\""))
        assertTrue(text.contains("\"minutes\": 12"))
        assertEquals(
            listOf(null, null, abend.toCompact(), viloma.toCompact(), twelve.toCompact()).sortedBy { it.orEmpty() },
            file.patterns.map { it.phases }.sortedBy { it.orEmpty() },
        )

        val decoded = PatternBackup.decode(text)!!
        assertEquals(0, decoded.invalid)
        val target = FakeSavedPatternDao()
        val targetRepo = SavedPatternsRepository(target)
        val written = mutableMapOf<String, PatternBackupOverrides>()
        assertEquals(ImportResult(added = 5, duplicates = 0), targetRepo.import(decoded.entries) { id, o -> written[id] = o })

        // Gleiche Zeilen (die Reihenfolge folgt dem Export, nicht dem Anlegen).
        assertEquals(source.rows.value.map { it.copy(id = 0) }.sortedBy { it.name }, target.rows.value.map { it.copy(id = 0) }.sortedBy { it.name })
        // Die Anpassungen hängen am Tagesmuster, auch wenn es im Ziel eine andere Id hat.
        val dailyId = target.rows.value.single { it.origin == SavedPatternEntity.ORIGIN_DAILY }.id
        assertEquals(mapOf("muster-$dailyId" to PatternBackupOverrides(duration = 7, inhale = 5)), written)
        val custom = targetRepo.patterns.first().single { it.exercise.name == "Abend" }
        assertEquals(PatternOrigin.CUSTOM, custom.origin)
        assertEquals(12, custom.minutes)
        val free = targetRepo.patterns.first().filter { it.exercise.name in setOf("Stufen", "Zwölf") }.map { it.pattern }.toSet()
        assertEquals(setOf(viloma, twelve), free)

        // Erneuter Import derselben Datei: alles Dubletten.
        assertEquals(ImportResult(added = 0, duplicates = 5), targetRepo.import(decoded.entries) { _, _ -> })
    }

    @Test
    fun `eine Datei von 1_6_8 ohne Herkunft und Dauer wird zu gespeicherten Tagesmustern`() = runBlocking {
        val decoded = PatternBackup.decode(V1_FILE_1_6_8)!!
        assertEquals(0, decoded.invalid)
        assertEquals(listOf(null, null), decoded.entries.map { it.origin })

        val dao = FakeSavedPatternDao()
        val written = mutableMapOf<String, PatternBackupOverrides>()
        SavedPatternsRepository(dao).import(decoded.entries) { id, o -> written[id] = o }

        assertEquals(listOf(SavedPatternEntity.ORIGIN_DAILY, SavedPatternEntity.ORIGIN_DAILY), dao.rows.value.map { it.origin })
        assertEquals(listOf(null, null), dao.rows.value.map { it.minutes })
        assertEquals(mapOf("muster-2" to PatternBackupOverrides(duration = 8)), written)
    }

    /** Dubletten zählen nach Zeitpunkt und Werten; eine freie Folge zählt normalisiert. */
    @Test
    fun `Import ueberspringt vorhandene und doppelte Muster`() = runBlocking {
        val dao = FakeSavedPatternDao()
        dao.insert(entity(1_000))
        val repo = SavedPatternsRepository(dao)
        repo.saveCustom(viloma, minutes = 5, name = "Stufen")
        val (four, free) = repo.exportFile(nowEpochMs = 0) { null }.patterns
        val patterns = listOf(
            four,
            four,
            four.copy(createdAtEpochMs = 5_000),
            free.copy(phases = free.phases!!.replace(",", " , ").uppercase()),
            free.copy(phases = "in:6,hold:6,out:6"),
        )

        val result = repo.import(PatternBackup.decode(PatternBackup.encode(PatternBackupFile(exportedAtEpochMs = 0, patterns = patterns)))!!.entries) { _, _ -> }

        assertEquals(ImportResult(added = 2, duplicates = 3), result)
        assertEquals(4, dao.rows.value.size)
        assertEquals(setOf(null, viloma.toCompact(), "in:6,hold:6,out:6"), dao.rows.value.map { it.phases }.toSet())
    }

    @Test
    fun `fremde oder zu grosse Dateien werden abgewiesen`() {
        assertNull(PatternBackup.decode("""{"format":"anderes","version":1,"exportedAtEpochMs":0,"patterns":[]}"""))
        assertNull(PatternBackup.decode("""{"format":"atemkraft-muster","version":99,"exportedAtEpochMs":0,"patterns":[]}"""))
        assertNull(PatternBackup.decode("kein json"))
        assertNull(PatternBackup.decode(" ".repeat(PatternBackup.MAX_BYTES + 1)))

        // Zu viele Muster: der Rest wird abgeschnitten und gezählt.
        val many = PatternBackup.decode(backup(*Array(PatternBackup.MAX_PATTERNS + 5) { entry("") }))!!
        assertEquals(PatternBackup.MAX_PATTERNS, many.entries.size)
        assertEquals(0, many.invalid)
        assertEquals(5, many.truncated)
    }

    /** Je Regel ein Vertreter; die Parser-Fälle der Folge stehen in `FreePatternTest`. */
    @Test
    fun `unplausible Eintraege fallen weg, der Rest bleibt`() = runBlocking {
        val bad = listOf(
            """{"name":" ","inhale":4.0,"exhale":6.0,"activating":false,"createdAtEpochMs":1}""",
            """{"name":"\u202Eretsum","inhale":4.0,"exhale":6.0,"activating":false,"createdAtEpochMs":1}""",
            // Wert außerhalb des Rasters, ruhig mit Ausatmen kürzer als Einatmen (Leitplanken)
            """{"name":"x","inhale":4.3,"exhale":6.0,"activating":false,"createdAtEpochMs":1}""",
            """{"name":"x","inhale":6.0,"exhale":4.0,"activating":false,"createdAtEpochMs":1}""",
            // Anpassung umgeht die Stepper-Grenzen, Dauer über 30 min, Zeitpunkt fehlt
            entry(""""overrides":{"hold":600}"""),
            entry(""""minutes":31"""),
            entry("", created = 0),
            // Folge ohne Ausatmen, mit 13 Phasen, Folge bei einem Tagesmuster
            entry(""""phases":"in:4,hold:3","origin":"custom""""),
            entry(""""phases":"${List(13) { if (it % 2 == 0) "in:2" else "out:2" }.joinToString(",")}","origin":"custom""""),
            entry(""""phases":"in:4,out:6","origin":"daily""""),
        )
        val good = listOf(entry(""), entry(""""minutes":30,"origin":"custom"""", created = 2), entry(""""origin":"zukunft"""", created = 3))

        val decoded = PatternBackup.decode(backup(*(bad + good).toTypedArray()))!!

        assertEquals(bad.size, decoded.invalid)
        assertEquals(listOf(null, 30, null), decoded.entries.map { it.minutes })
        val dao = FakeSavedPatternDao()
        SavedPatternsRepository(dao).import(decoded.entries) { _, _ -> }
        // Ohne Herkunft (vor dem Editor) ein Tagesmuster, unbekannte Herkunft ein eigenes Muster.
        assertEquals(
            listOf(SavedPatternEntity.ORIGIN_DAILY, SavedPatternEntity.ORIGIN_CUSTOM, SavedPatternEntity.ORIGIN_CUSTOM),
            dao.rows.value.map { it.origin },
        )
    }

    private companion object {
        /** Export aus Version 1.6.8 (vor dem Muster-Editor): ohne `origin` und `minutes`. */
        val V1_FILE_1_6_8 = """
            {
                "format": "atemkraft-muster",
                "version": 1,
                "exportedAtEpochMs": 1760000000000,
                "patterns": [
                    {
                        "name": "Muster 4·2·6",
                        "inhale": 4.0,
                        "holdFull": 2.0,
                        "exhale": 6.0,
                        "holdEmpty": null,
                        "activating": false,
                        "createdAtEpochMs": 1759000000000,
                        "overrides": null
                    },
                    {
                        "name": "Muster 4·3",
                        "inhale": 4.0,
                        "holdFull": null,
                        "exhale": 3.0,
                        "holdEmpty": null,
                        "activating": true,
                        "createdAtEpochMs": 1759500000000,
                        "overrides": {
                            "duration": 8,
                            "inhale": null,
                            "hold": null,
                            "exhale": null
                        }
                    }
                ]
            }
        """.trimIndent()
    }
}
