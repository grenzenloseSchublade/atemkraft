package app.atemkraft.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.atemkraft.data.local.SavedPatternEntity
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.Feeling
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.SessionKind
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.toFreePattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate

/** SEC-STORE-01/-02: Export/Import gespeicherter Muster – Round-Trip und Import als nicht vertrauenswürdige Eingabe. */
class PatternBackupTest {

    @get:Rule
    val folder = TemporaryFolder()

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

        // Das gespeicherte Tagesmuster erkennt die App an Herkunft und Zahlen, nie am Namen (CODE-05):
        // auch umbenannt aus einer alten Datei ohne Herkunft; ein eigenes Muster mit gleichen Zahlen nicht.
        val daily = RandomPatternGenerator.forEpochDay(LocalDate.of(2026, 10, 5).toEpochDay())
        val renamed = with(daily.spec) {
            val holds = listOfNotNull(holdFull?.let { "\"holdFull\":$it" }, holdEmpty?.let { "\"holdEmpty\":$it" }).joinToString("") { "$it," }
            """{"name":"Lieblingsmuster","inhale":$inhale,$holds"exhale":$exhale,"activating":$activating,"createdAtEpochMs":5}"""
        }
        val dailyRepo = SavedPatternsRepository(FakeSavedPatternDao())
        dailyRepo.saveCustom(daily.spec.toFreePattern(), minutes = 5, name = "Gleiche Zahlen")
        assertFalse(dailyRepo.isDailySaved(daily, dailyRepo.patterns.first()))
        dailyRepo.import(PatternBackup.decode(backup(renamed))!!.entries) { _, _ -> }
        assertTrue(dailyRepo.isDailySaved(daily, dailyRepo.patterns.first()))
        dailyRepo.save(daily)
        assertEquals(2, dailyRepo.patterns.first().size)
        dailyRepo.unsave(daily)
        assertEquals(listOf("Gleiche Zahlen"), dailyRepo.patterns.first().map { it.exercise.name })

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

    /**
     * Sicherung „alles“ (Version 2): Logbuch samt Befinden, Markierungen und Einstellungen im Rundlauf;
     * zusammengeführt ohne Dubletten, Einstellungen nur auf Wunsch, die Erinnerung bleibt am Gerät;
     * eine Datei der Version 1 (nur Muster) bleibt lesbar, Unplausibles im Logbuch fällt weg.
     */
    @Test
    fun `Sicherung alles - Rundlauf, Zusammenführen und alte Musterdatei`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            fun settings(name: String) = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "$name.preferences_pb") })
            val seen = SessionLogEntry(
                exerciseId = "resonanz",
                exerciseName = "Resonanz-Atmung",
                family = BreathingFamily.DOWNREGULATE,
                startedAtEpochMs = 1_760_000_000_000,
                durationMs = 300_000,
                roundsCompleted = 30,
                feeling = Feeling.CALMER,
            )
            val meditation = SessionLogEntry(
                exerciseId = "meditation",
                exerciseName = "Meditation",
                family = null,
                startedAtEpochMs = 1_760_100_000_000,
                durationMs = 600_000,
                roundsCompleted = 0,
                kind = SessionKind.MEDITATION,
            )

            val sourceSettings = settings("quelle")
            sourceSettings.setToneVolume(ToneVolume.QUIET)
            sourceSettings.setExerciseMarked("resonanz", true)
            sourceSettings.setExerciseIntervals("resonanz", 7, null, null, null)
            sourceSettings.setReminderEnabled(true)
            val sourceLog = LogbookRepository(FakeLogbookDao()).apply {
                append(seen)
                append(meditation)
            }
            val sourceSaved = SavedPatternsRepository(FakeSavedPatternDao()).apply { saveCustom(viloma, minutes = 7, name = "Stufen") }
            val file = ByteArrayOutputStream()
            assertEquals(
                PatternTransferService.ExportSummary(patterns = 1, logbook = 2),
                PatternTransferService(sourceSaved, sourceSettings, sourceLog).export(1_000) { file },
            )

            // Ziel: derselbe Resonanz-Eintrag ist schon da (ohne Befinden), sonst leer.
            val targetSettings = settings("ziel")
            val targetDao = FakeLogbookDao()
            val targetLog = LogbookRepository(targetDao).apply { append(seen.copy(feeling = null)) }
            val target = PatternTransferService(SavedPatternsRepository(FakeSavedPatternDao()), targetSettings, targetLog)
            val read = target.read { ByteArrayInputStream(file.toByteArray()) } as PatternTransferService.ReadOutcome.Preview
            assertEquals(listOf(seen.copy(id = 0), meditation), read.decoded.logbook)
            assertEquals(setOf("resonanz"), read.decoded.marked)

            // Ohne Einstellungen: Muster und Meditation neu, Resonanz vorhanden; Lautstärke bleibt.
            assertEquals(PatternTransferService.ApplyResult(added = 2, duplicates = 1, settingsApplied = false), target.apply(read.decoded, withSettings = false))
            assertEquals(ToneVolume.MEDIUM, targetSettings.cueSettings.first().volume)
            assertEquals(setOf("resonanz"), targetSettings.markedExercises.first())
            assertEquals(2, targetDao.rows.size)
            // Mit Einstellungen: alles schon da, Lautstärke und Anpassung übernommen, Erinnerung nicht.
            assertEquals(PatternTransferService.ApplyResult(added = 0, duplicates = 3, settingsApplied = true), target.apply(read.decoded, withSettings = true))
            assertEquals(ToneVolume.QUIET, targetSettings.cueSettings.first().volume)
            assertEquals(7, targetSettings.exerciseIntervals("resonanz").first()?.duration)
            assertFalse(targetSettings.reminderSettings.first().enabled)
        } finally {
            scope.cancel()
        }

        // Version 1 (nur Muster) bleibt lesbar; ein Logbuch-Eintrag mit unbekannter Art fällt weg.
        assertEquals(1, PatternBackup.decode(backup(entry("")))!!.entries.size)
        val log = """[{"exerciseId":"x","exerciseName":"X","kind":"ANDERS","startedAtEpochMs":1,"durationMs":1},""" +
            """{"exerciseId":"meditation","exerciseName":"Meditation","kind":"MEDITATION","startedAtEpochMs":1,"durationMs":1,"feeling":"NEU"}]"""
        val v2 = PatternBackup.decode("""{"format":"atemkraft-muster","version":2,"exportedAtEpochMs":0,"patterns":[],"logbook":$log}""")!!
        assertEquals(1, v2.invalid)
        assertNull(v2.logbook.single().feeling)
    }
}
