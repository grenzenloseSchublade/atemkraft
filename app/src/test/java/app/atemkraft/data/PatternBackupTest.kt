package app.atemkraft.data

import app.atemkraft.data.local.SavedPatternDao
import app.atemkraft.data.local.SavedPatternEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** SEC-STORE-01/-02: Export/Import gespeicherter Muster – Round-Trip und Import als nicht vertrauenswürdige Eingabe. */
class PatternBackupTest {

    private class FakeDao : SavedPatternDao {
        val rows = MutableStateFlow<List<SavedPatternEntity>>(emptyList())
        private var nextId = 1L

        override suspend fun insert(pattern: SavedPatternEntity): Long {
            val id = nextId++
            rows.value = rows.value + pattern.copy(id = id)
            return id
        }

        override fun all(): Flow<List<SavedPatternEntity>> = rows

        override suspend fun snapshot(): List<SavedPatternEntity> = rows.value.sortedBy { it.createdAtEpochMs }

        override suspend fun delete(id: Long) {
            rows.value = rows.value.filterNot { it.id == id }
        }
    }

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
    fun roundTripKeepsValuesAndOverrides() = runBlocking {
        val source = FakeDao()
        source.insert(entity(1_000))
        source.insert(entity(2_000, inhale = 5.5, holdFull = null))
        val sourceRepo = SavedPatternsRepository(source)
        val overrides = mapOf("muster-1" to IntervalOverrides(duration = 7, inhale = 5))
        val file = sourceRepo.exportFile(nowEpochMs = 3_000) { overrides[it] }

        val decoded = PatternBackup.decode(PatternBackup.encode(file))!!
        assertEquals(0, decoded.invalid)

        val target = FakeDao()
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
    fun importSkipsExistingAndDuplicatesInFile() = runBlocking {
        val dao = FakeDao()
        dao.insert(entity(1_000))
        val repo = SavedPatternsRepository(dao)
        val file = repo.exportFile(nowEpochMs = 0) { null }
        val twice = file.copy(patterns = file.patterns + file.patterns + file.patterns.map { it.copy(createdAtEpochMs = 5_000) })

        val result = repo.import(PatternBackup.decode(PatternBackup.encode(twice))!!.entries) { _, _ -> }

        assertEquals(ImportResult(added = 1, duplicates = 2), result)
        assertEquals(2, dao.rows.value.size)
    }

    @Test
    fun rejectsForeignOrOversizedFiles() {
        assertNull(PatternBackup.decode("""{"format":"anderes","version":1,"exportedAtEpochMs":0,"patterns":[]}"""))
        assertNull(PatternBackup.decode("""{"format":"atemkraft-muster","version":99,"exportedAtEpochMs":0,"patterns":[]}"""))
        assertNull(PatternBackup.decode("kein json"))
        assertNull(PatternBackup.decode(" ".repeat(PatternBackup.MAX_BYTES + 1)))
    }

    @Test
    fun dropsImplausibleEntriesButKeepsTheRest() {
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
    fun capsNumberOfPatterns() {
        val one = """{"name":"M","inhale":4.0,"exhale":6.0,"activating":false,"createdAtEpochMs":1}"""
        val text = """{"format":"atemkraft-muster","version":1,"exportedAtEpochMs":0,
            "patterns":[${List(PatternBackup.MAX_PATTERNS + 5) { one }.joinToString(",")}]}"""

        val decoded = PatternBackup.decode(text)!!

        assertEquals(PatternBackup.MAX_PATTERNS, decoded.entries.size)
        assertEquals(0, decoded.invalid)
        assertEquals(5, decoded.truncated)
    }

    @Test
    fun rejectsPatternsOutsideGuardrailsAndOverridesOutsideStepperLimits() {
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
    fun rejectsNamesWithHiddenOrBreakingCharacters() {
        val names = listOf("\u202Eretsum", "Muster\nzwei", "\u200B", "\u0000x", "x\u2028y")
        val text = """{"format":"atemkraft-muster","version":1,"exportedAtEpochMs":0,"patterns":[${
            names.joinToString(",") { """{"name":"$it","inhale":4.0,"exhale":6.0,"activating":false,"createdAtEpochMs":1}""" }
        }]}"""

        val decoded = PatternBackup.decode(text)!!

        assertEquals(0, decoded.entries.size)
        assertEquals(names.size, decoded.invalid)
    }
}
