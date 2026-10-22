package app.atemkraft.data

import app.atemkraft.data.local.SavedPatternEntity
import app.atemkraft.domain.PatternOrigin
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.RandomPatternGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** SEC-STORE-01/-02: Export/Import gespeicherter Muster – Round-Trip und Import als nicht vertrauenswürdige Eingabe. */
class PatternBackupTest {

    private fun entity(created: Long, inhale: Double = 4.0, holdFull: Double? = 2.0) = SavedPatternEntity(
        name = "Muster $created",
        inhale = inhale,
        holdFull = holdFull,
        exhale = 6.0,
        holdEmpty = null,
        activating = false,
        createdAtEpochMs = created,
    )

    @Test
    fun `Export und Import ergeben dieselben Werte und Anpassungen`() = runBlocking {
        val source = FakeSavedPatternDao()
        source.insert(entity(1_000))
        source.insert(entity(2_000, inhale = 5.5, holdFull = null))
        val sourceRepo = SavedPatternsRepository(source)
        val overrides = mapOf("muster-1" to IntervalOverrides(duration = 7, inhale = 5))
        val file = sourceRepo.exportFile(nowEpochMs = 3_000) { overrides[it] }

        val decoded = PatternBackup.decode(PatternBackup.encode(file))!!
        assertEquals(0, decoded.invalid)

        val target = FakeSavedPatternDao()
        val written = mutableMapOf<String, PatternBackupOverrides>()
        val result = SavedPatternsRepository(target).import(decoded.entries) { id, o -> written[id] = o }

        assertEquals(ImportResult(added = 2, duplicates = 0), result)
        assertEquals(
            source.rows.value.map { it.copy(id = 0) },
            target.rows.value.map { it.copy(id = 0) },
        )
        assertEquals(mapOf("muster-1" to PatternBackupOverrides(duration = 7, inhale = 5)), written)
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

    @Test
    fun `eigenes Muster mit Dauer uebersteht Export und Import, Version bleibt 1`() = runBlocking {
        val source = FakeSavedPatternDao()
        val sourceRepo = SavedPatternsRepository(source)
        sourceRepo.save(RandomPatternGenerator.forDate(LocalDate.of(2026, 10, 5)))
        sourceRepo.saveCustom(PatternSpec(4.0, 1.0, 6.0, null, false), minutes = 12, name = "Abend")
        val file = sourceRepo.exportFile(nowEpochMs = 3_000) { null }
        val text = PatternBackup.encode(file)

        assertEquals(1, file.version)
        assertTrue(text.contains("\"origin\": \"custom\""))
        assertTrue(text.contains("\"minutes\": 12"))

        val target = FakeSavedPatternDao()
        val targetRepo = SavedPatternsRepository(target)
        val result = targetRepo.import(PatternBackup.decode(text)!!.entries) { _, _ -> }

        assertEquals(ImportResult(added = 2, duplicates = 0), result)
        assertEquals(source.rows.value.map { it.copy(id = 0) }, target.rows.value.map { it.copy(id = 0) })
        val custom = targetRepo.patterns.first().single { it.origin == PatternOrigin.CUSTOM }
        assertEquals(12, custom.minutes)
        assertEquals("Abend", custom.exercise.name)

        // Erneuter Import derselben Datei: alles Dubletten.
        assertEquals(
            ImportResult(added = 0, duplicates = 2),
            targetRepo.import(PatternBackup.decode(text)!!.entries) { _, _ -> },
        )
    }

    @Test
    fun `Dauer ausserhalb 1 bis 30 Minuten wird verworfen, unbekannte Herkunft wird eigenes Muster`() = runBlocking {
        fun entry(extra: String) = """{"name":"M","inhale":4.0,"exhale":6.0,"activating":false,"createdAtEpochMs":1$extra}"""
        val text = """{"format":"atemkraft-muster","version":1,"exportedAtEpochMs":0,"patterns":[
            ${entry(""","minutes":0""")},
            ${entry(""","minutes":31""")},
            ${entry(""","minutes":30,"origin":"custom"""")},
            ${entry(""","origin":"zukunft"""").replace("\"createdAtEpochMs\":1", "\"createdAtEpochMs\":2")}
        ]}"""

        val decoded = PatternBackup.decode(text)!!

        assertEquals(2, decoded.invalid)
        assertEquals(listOf(30, null), decoded.entries.map { it.minutes })

        val dao = FakeSavedPatternDao()
        SavedPatternsRepository(dao).import(decoded.entries) { _, _ -> }
        assertEquals(
            listOf(SavedPatternEntity.ORIGIN_CUSTOM, SavedPatternEntity.ORIGIN_CUSTOM),
            dao.rows.value.map { it.origin },
        )
    }

    @Test
    fun `cleanName macht Namen importfest`() {
        assertEquals("Morgen frisch", PatternBackup.cleanName(" Morgen\n\tfrisch\u200B "))
        assertEquals("", PatternBackup.cleanName("\u202E \u0000"))
        assertEquals(PatternBackup.MAX_NAME, PatternBackup.cleanName("y".repeat(200)).length)
        assertEquals("a b", PatternBackup.cleanName("a" + " ".repeat(5) + "b"))
    }

    @Test
    fun `Import ueberspringt vorhandene und doppelte Muster`() = runBlocking {
        val dao = FakeSavedPatternDao()
        dao.insert(entity(1_000))
        val repo = SavedPatternsRepository(dao)
        val file = repo.exportFile(nowEpochMs = 0) { null }
        val twice = file.copy(patterns = file.patterns + file.patterns + file.patterns.map { it.copy(createdAtEpochMs = 5_000) })

        val result = repo.import(PatternBackup.decode(PatternBackup.encode(twice))!!.entries) { _, _ -> }

        assertEquals(ImportResult(added = 1, duplicates = 2), result)
        assertEquals(2, dao.rows.value.size)
    }

    @Test
    fun `fremde oder zu grosse Dateien werden abgewiesen`() {
        assertNull(PatternBackup.decode("""{"format":"anderes","version":1,"exportedAtEpochMs":0,"patterns":[]}"""))
        assertNull(PatternBackup.decode("""{"format":"atemkraft-muster","version":99,"exportedAtEpochMs":0,"patterns":[]}"""))
        assertNull(PatternBackup.decode("kein json"))
        assertNull(PatternBackup.decode(" ".repeat(PatternBackup.MAX_BYTES + 1)))
    }

    @Test
    fun `unplausible Eintraege fallen weg, der Rest bleibt`() {
        val ok = """{"name":"Muster","inhale":4.0,"exhale":6.0,"activating":false,"createdAtEpochMs":1}"""
        val bad = listOf(
            """{"name":" ","inhale":4.0,"exhale":6.0,"activating":false,"createdAtEpochMs":1}""",
            """{"name":"x","inhale":0.0,"exhale":6.0,"activating":false,"createdAtEpochMs":1}""",
            """{"name":"x","inhale":4.3,"exhale":6.0,"activating":false,"createdAtEpochMs":1}""",
            """{"name":"x","inhale":4.0,"exhale":99.0,"activating":false,"createdAtEpochMs":1}""",
            """{"name":"x","inhale":4.0,"holdFull":-1.0,"exhale":6.0,"activating":false,"createdAtEpochMs":1}""",
            """{"name":"x","inhale":4.0,"exhale":6.0,"activating":false,"createdAtEpochMs":0}""",
            """{"name":"x","inhale":4.0,"exhale":6.0,"activating":false,"createdAtEpochMs":1,"overrides":{"duration":-3}}""",
        )
        val text = """{"format":"atemkraft-muster","version":1,"exportedAtEpochMs":0,"zukunft":true,
            "patterns":[$ok,${bad.joinToString(",")}]}"""

        val decoded = PatternBackup.decode(text)!!

        assertEquals(1, decoded.entries.size)
        assertEquals(bad.size, decoded.invalid)
    }

    @Test
    fun `Anzahl der Muster ist begrenzt`() {
        val one = """{"name":"M","inhale":4.0,"exhale":6.0,"activating":false,"createdAtEpochMs":1}"""
        val text = """{"format":"atemkraft-muster","version":1,"exportedAtEpochMs":0,
            "patterns":[${List(PatternBackup.MAX_PATTERNS + 5) { one }.joinToString(",")}]}"""

        val decoded = PatternBackup.decode(text)!!

        assertEquals(PatternBackup.MAX_PATTERNS, decoded.entries.size)
        assertEquals(0, decoded.invalid)
        assertEquals(5, decoded.truncated)
    }

    @Test
    fun `Muster ausserhalb der Leitplanken und Anpassungen ausserhalb der Stepper werden abgewiesen`() {
        fun entry(fields: String) = """{"name":"M","activating":false,"createdAtEpochMs":1,$fields}"""
        val bad = listOf(
            // 30 s Halten: läge außerhalb der Generator-Leitplanken (Halten ≤ 4 s)
            entry(""""inhale":4.0,"holdFull":30.0,"exhale":4.0,"holdEmpty":30.0"""),
            // ruhig, aber Ausatmen kürzer als Einatmen
            entry(""""inhale":6.0,"exhale":4.0"""),
            // Anpassung umgeht die Stepper-Grenzen (Halten 1..20 s, Einatmen 2..12 s)
            entry(""""inhale":4.0,"exhale":6.0,"overrides":{"hold":600}"""),
            entry(""""inhale":4.0,"exhale":6.0,"overrides":{"inhale":0}"""),
            // aktivierend mit Halten nach dem Ausatmen
            """{"name":"M","activating":true,"createdAtEpochMs":1,"inhale":4.0,"exhale":3.0,"holdEmpty":1.0}""",
        )
        val text = """{"format":"atemkraft-muster","version":1,"exportedAtEpochMs":0,"patterns":[${bad.joinToString(",")}]}"""

        val decoded = PatternBackup.decode(text)!!

        assertEquals(0, decoded.entries.size)
        assertEquals(bad.size, decoded.invalid)
    }

    @Test
    fun `Namen mit Steuer- oder Formatzeichen werden abgewiesen`() {
        val names = listOf("\u202Eretsum", "Muster\nzwei", "\u200B", "\u0000x", "x\u2028y")
        val text = """{"format":"atemkraft-muster","version":1,"exportedAtEpochMs":0,"patterns":[${
            names.joinToString(",") { """{"name":"$it","inhale":4.0,"exhale":6.0,"activating":false,"createdAtEpochMs":1}""" }
        }]}"""

        val decoded = PatternBackup.decode(text)!!

        assertEquals(0, decoded.entries.size)
        assertEquals(names.size, decoded.invalid)
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
