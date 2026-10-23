package app.atemkraft.data

import app.atemkraft.data.local.SavedPatternEntity
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.RandomPatternGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** SEC-STORE-01/-02: Export/Import gespeicherter Muster – Round-Trip und Import als nicht vertrauenswürdige Eingabe. */
class PatternBackupTest {

    /** Viloma I: drei Stufen Einatmen mit Pausen, dann Ausatmen. */
    private val viloma = FreePatternTemplates.all.single { it.label == "Viloma I" }.pattern

    private fun entry(fields: String, created: Int = 1) = """{"name":"M","inhale":4.0,"exhale":6.0,"activating":false,"createdAtEpochMs":$created${if (fields.isEmpty()) "" else ",$fields"}}"""

    private fun backup(vararg entries: String) = """{"format":"atemkraft-muster","version":1,"exportedAtEpochMs":0,"zukunft":true,"patterns":[${entries.joinToString(",")}]}"""

    @Test
    fun `Export und Import ergeben dieselben Muster, Unplausibles faellt weg`() = runBlocking {
        // Rundlauf: Tagesmuster mit Anpassung und eigene freie Folge.
        val source = FakeSavedPatternDao()
        val sourceRepo = SavedPatternsRepository(source)
        sourceRepo.save(RandomPatternGenerator.forEpochDay(LocalDate.of(2026, 10, 5).toEpochDay()))
        sourceRepo.saveCustom(viloma, minutes = 7, name = "Stufen")
        val overrides = mapOf("muster-1" to IntervalOverrides(duration = 7, inhale = 5))
        val decoded = PatternBackup.decode(PatternBackup.encode(sourceRepo.exportFile(nowEpochMs = 3_000) { overrides[it] }))!!
        assertEquals(0, decoded.invalid)

        val target = FakeSavedPatternDao()
        val targetRepo = SavedPatternsRepository(target)
        val written = mutableMapOf<String, PatternBackupOverrides>()
        assertEquals(ImportResult(added = 2, duplicates = 0), targetRepo.import(decoded.entries) { id, o -> written[id] = o })
        assertEquals(source.rows.value.map { it.copy(id = 0) }.sortedBy { it.name }, target.rows.value.map { it.copy(id = 0) }.sortedBy { it.name })
        // Die Anpassung hängt am Tagesmuster, auch wenn es im Ziel eine andere Id hat.
        val dailyId = target.rows.value.single { it.origin == SavedPatternEntity.ORIGIN_DAILY }.id
        assertEquals(mapOf("muster-$dailyId" to PatternBackupOverrides(duration = 7, inhale = 5)), written)
        assertEquals(viloma, targetRepo.patterns.first().single { it.exercise.name == "Stufen" }.pattern)
        // Erneuter Import derselben Datei: alles Dubletten.
        assertEquals(ImportResult(added = 0, duplicates = 2), targetRepo.import(decoded.entries) { _, _ -> })

        // Fremde, unbekannte oder zu große Dateien werden ganz abgewiesen.
        assertNull(PatternBackup.decode("""{"format":"anderes","version":1,"exportedAtEpochMs":0,"patterns":[]}"""))
        assertNull(PatternBackup.decode("""{"format":"atemkraft-muster","version":99,"exportedAtEpochMs":0,"patterns":[]}"""))
        assertNull(PatternBackup.decode("kein json"))
        assertNull(PatternBackup.decode(" ".repeat(PatternBackup.MAX_BYTES + 1)))

        // Unplausible Einträge fallen einzeln weg, der Rest bleibt.
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
            // Folge ohne Ausatmen und mit 13 Phasen
            entry(""""phases":"in:4,hold:3","origin":"custom""""),
            entry(""""phases":"${List(13) { if (it % 2 == 0) "in:2" else "out:2" }.joinToString(",")}","origin":"custom""""),
        )
        val checked = PatternBackup.decode(backup(*(bad + entry("")).toTypedArray()))!!
        assertEquals(bad.size, checked.invalid)
        assertEquals(1, checked.entries.size)
    }
}
