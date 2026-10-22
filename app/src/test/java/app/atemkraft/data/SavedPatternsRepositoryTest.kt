package app.atemkraft.data

import app.atemkraft.data.local.SavedPatternEntity
import app.atemkraft.domain.PatternOrigin
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.defaultMinutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

/**
 * Gespeicherte Muster über das Repository: Lesezeichen am „Muster des Tages“ und eigene Muster
 * aus dem Editor (anlegen, bearbeiten, Herkunft, Dauer, Cache).
 */
class SavedPatternsRepositoryTest {

    private val daily = RandomPatternGenerator.forDate(LocalDate.of(2026, 10, 5))
    private val other = RandomPatternGenerator.forDate(LocalDate.of(2026, 10, 6))

    /** ruhig, mit Halten: 4·1·6 (Vorschlag 0 des Editors). */
    private val calm = PatternSpec(inhale = 4.0, holdFull = 1.0, exhale = 6.0, holdEmpty = null, activating = false)

    /** sanft aktivierend: 4·3. */
    private val active = PatternSpec(inhale = 4.0, holdFull = null, exhale = 3.0, holdEmpty = null, activating = true)

    @Test
    fun `Doppeltipp auf das Lesezeichen legt das Tagesmuster nur einmal an`() = runBlocking {
        val dao = FakeSavedPatternDao()
        val repo = SavedPatternsRepository(dao)

        repo.save(daily)
        repo.save(daily)

        assertEquals(listOf(repo.savedName(daily)), dao.rows.value.map { it.name })
        assertEquals(SavedPatternEntity.ORIGIN_DAILY, dao.rows.value.single().origin)
        assertNull(dao.rows.value.single().minutes)
    }

    @Test
    fun `Abwaehlen nimmt nur das Tagesmuster heraus und laesst andere Muster stehen`() = runBlocking {
        val dao = FakeSavedPatternDao()
        val repo = SavedPatternsRepository(dao)
        assertNotEquals("Testdaten brauchen zwei verschiedene Namen", repo.savedName(daily), repo.savedName(other))
        repo.save(other)
        repo.save(daily)

        repo.unsave(daily)

        assertEquals(listOf(repo.savedName(other)), dao.rows.value.map { it.name })
    }

    @Test
    fun `saveCustom liefert sofort eine Id, die byExerciseId aufloest`() = runBlocking {
        val repo = SavedPatternsRepository(FakeSavedPatternDao())

        val saved = repo.saveCustom(calm, minutes = 7, name = "Abendmuster")

        assertTrue(saved.id > 0)
        assertEquals(PatternOrigin.CUSTOM, saved.origin)
        assertEquals(7, saved.minutes)
        val exercise = repo.byExerciseId("muster-${saved.id}")
        assertNotNull(exercise)
        assertEquals("Abendmuster", exercise!!.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `saveCustom lehnt ein Muster ausserhalb der Erzeugungsregeln ab`() {
        runBlocking {
            // 4,5 s: in den Import-Leitplanken, aber keine ganze Sekunde
            SavedPatternsRepository(FakeSavedPatternDao()).saveCustom(calm.copy(inhale = 4.5), minutes = 5, name = "")
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `saveCustom lehnt eine Dauer ausserhalb 1 bis 30 Minuten ab`() {
        runBlocking { SavedPatternsRepository(FakeSavedPatternDao()).saveCustom(calm, minutes = 31, name = "") }
    }

    @Test
    fun `leerer Name wird zum Vorschlagsnamen, langer Name wird auf 80 Zeichen gekuerzt`() = runBlocking {
        val dao = FakeSavedPatternDao()
        val repo = SavedPatternsRepository(dao)

        val blank = repo.saveCustom(calm, minutes = 5, name = "   ")
        val long = repo.saveCustom(calm, minutes = 5, name = "x".repeat(120))
        val broken = repo.saveCustom(active, minutes = 5, name = "  Morgen\nfrisch‮ ")

        assertEquals("Eigenes Muster 4·1·6", blank.exercise.name)
        assertEquals(PatternBackup.MAX_NAME, long.exercise.name.length)
        assertEquals("Morgen frisch", broken.exercise.name)
        assertEquals(listOf("Eigenes Muster 4·1·6", "x".repeat(80), "Morgen frisch"), dao.rows.value.map { it.name })
    }

    @Test
    fun `update behaelt Id und Anlagedatum, macht das Muster eigen und loescht die Anpassungen einmal`() = runBlocking {
        val dao = FakeSavedPatternDao()
        val cleared = mutableListOf<String>()
        val repo = SavedPatternsRepository(dao, clearOverrides = { cleared += it })
        repo.save(daily)
        val before = dao.rows.value.single()

        val updated = repo.update(before.id, calm, minutes = 12, name = "")

        val after = dao.rows.value.single()
        assertEquals(before.id, after.id)
        assertEquals(before.createdAtEpochMs, after.createdAtEpochMs)
        assertEquals(SavedPatternEntity.ORIGIN_CUSTOM, after.origin)
        assertEquals(12, after.minutes)
        assertEquals("Eigenes Muster 4·1·6", after.name)
        assertEquals(listOf("muster-${before.id}"), cleared)
        assertEquals(PatternOrigin.CUSTOM, updated!!.origin)
        assertEquals(calm, updated.spec)
        assertEquals(updated.exercise, repo.byExerciseId("muster-${before.id}"))
    }

    @Test
    fun `update eines geloeschten Musters liefert null und loescht keine Anpassungen`() = runBlocking {
        val cleared = mutableListOf<String>()
        val repo = SavedPatternsRepository(FakeSavedPatternDao(), clearOverrides = { cleared += it })

        assertNull(repo.update(42, calm, minutes = 5, name = "x"))
        assertTrue(cleared.isEmpty())
    }

    @Test
    fun `Lesezeichen am Tagesmuster laesst ein eigenes Muster mit gleichen Zahlen und Namen unberuehrt`() = runBlocking {
        val dao = FakeSavedPatternDao()
        val repo = SavedPatternsRepository(dao)
        val custom = repo.saveCustom(daily.spec, minutes = 5, name = repo.savedName(daily))

        assertFalse(repo.isDailySaved(daily, repo.patterns.first()))
        repo.save(daily)
        assertEquals(2, dao.rows.value.size)
        assertTrue(repo.isDailySaved(daily, repo.patterns.first()))

        repo.unsave(daily)

        assertEquals(listOf(custom.id), dao.rows.value.map { it.id })
        assertEquals(SavedPatternEntity.ORIGIN_CUSTOM, dao.rows.value.single().origin)
        assertFalse(repo.isDailySaved(daily, repo.patterns.first()))
    }

    @Test
    fun `eigene Muster bekommen die Texte eigener Muster und ihre Dauer`() = runBlocking {
        val repo = SavedPatternsRepository(FakeSavedPatternDao())

        for (minutes in listOf(1, 3, 5, 10, 30)) {
            val saved = repo.saveCustom(calm, minutes = minutes, name = "Mein Muster")
            val expected = RandomPatternGenerator.exerciseFrom(
                calm,
                "muster-${saved.id}",
                "Mein Muster",
                PatternOrigin.CUSTOM,
                minutes,
            )
            assertEquals(expected, saved.exercise)
            assertTrue("$minutes min", abs(saved.exercise.defaultMinutes() - minutes) <= 1)
        }
    }

    @Test
    fun `gespeicherte Tagesmuster bekommen die Texte gespeicherter Tagesmuster`() = runBlocking {
        val repo = SavedPatternsRepository(FakeSavedPatternDao())
        repo.save(daily)

        val saved = repo.patterns.first().single()

        assertEquals(PatternOrigin.SAVED_DAILY, saved.origin)
        assertEquals(
            RandomPatternGenerator.exerciseFrom(daily.spec, "muster-${saved.id}", repo.savedName(daily), PatternOrigin.SAVED_DAILY),
            saved.exercise,
        )
    }

    @Test
    fun `unbekannte Herkunft in der DB gilt als eigenes Muster`() = runBlocking {
        val dao = FakeSavedPatternDao()
        dao.insert(row(origin = "zukunft"))

        val saved = SavedPatternsRepository(dao).patterns.first().single()

        assertEquals(PatternOrigin.CUSTOM, saved.origin)
    }

    @Test
    fun `loadForEdit liefert das Muster, observe folgt dem Bearbeiten`() = runBlocking {
        val repo = SavedPatternsRepository(FakeSavedPatternDao())
        val saved = repo.saveCustom(calm, minutes = 5, name = "A")

        assertEquals(saved, repo.loadForEdit(saved.id))
        assertNull(repo.loadForEdit(999))
        assertEquals("A", repo.observe("muster-${saved.id}").first()?.name)

        repo.update(saved.id, active, minutes = 8, name = "B")

        val observed = repo.observe("muster-${saved.id}").first()
        assertEquals("B", observed?.name)
        assertTrue(observed!!.activating)
        assertNull(repo.observe("muster-999").first())
        assertNull(repo.observe("box-4-4-4-4").first())
        assertNull(repo.observe(RandomPatternGenerator.DRAFT_ID).first())
    }

    @Test
    fun `ein spaet eintreffender alter DB-Stand ueberschreibt den Cache nicht`() = runBlocking {
        val db = MutableSharedFlow<List<SavedPatternEntity>>(replay = 1)
        val dao = object : FakeSavedPatternDao() {
            override fun all(): Flow<List<SavedPatternEntity>> = db
        }
        val repo = SavedPatternsRepository(dao, scope = CoroutineScope(Dispatchers.Unconfined))
        db.emit(emptyList())

        // Anlegen: sofort auflösbar, auch wenn die DB danach noch den Stand davor liefert.
        val saved = repo.saveCustom(calm, minutes = 5, name = "A")
        val exerciseId = "muster-${saved.id}"
        db.emit(emptyList())
        assertEquals("A", repo.byExerciseId(exerciseId)?.name)
        db.emit(dao.rows.value)
        assertEquals("A", repo.byExerciseId(exerciseId)?.name)

        // Bearbeiten: der alte Stand mit „A“ kommt noch einmal an, der Cache bleibt bei „B“.
        val stale = dao.rows.value
        repo.update(saved.id, calm.copy(exhale = 7.0), minutes = 5, name = "B")
        db.emit(stale)
        assertEquals("B", repo.byExerciseId(exerciseId)?.name)
        db.emit(dao.rows.value)
        assertEquals("B", repo.byExerciseId(exerciseId)?.name)

        // Löschen: ein alter Stand mit dem Muster holt es nicht zurück.
        val beforeDelete = dao.rows.value
        repo.delete(saved.id)
        db.emit(beforeDelete)
        assertNull(repo.byExerciseId(exerciseId))
        db.emit(dao.rows.value)
        assertNull(repo.byExerciseId(exerciseId))

        // Bestätigte Stände folgen danach wieder ganz der DB (Änderung von außen, z. B. Import).
        dao.insert(row(origin = SavedPatternEntity.ORIGIN_CUSTOM))
        db.emit(dao.rows.value)
        assertEquals(1, dao.rows.value.size)
        assertNotNull(repo.byExerciseId("muster-${dao.rows.value.single().id}"))
    }

    private fun row(origin: String) = SavedPatternEntity(
        name = "Muster",
        inhale = 4.0,
        holdFull = null,
        exhale = 6.0,
        holdEmpty = null,
        activating = false,
        createdAtEpochMs = 1_000,
        origin = origin,
    )

    @Test
    fun `Bearbeiten startet mit der Dauer der Detailseite, update schreibt sie in die Spalte`() = runBlocking {
        val dao = FakeSavedPatternDao()
        val overrides = mutableMapOf<String, Int>()
        val repo = SavedPatternsRepository(dao, clearOverrides = { overrides.remove(it) }, overrideMinutes = { overrides[it] })
        repo.save(daily)
        val id = dao.rows.value.single().id
        // Tagesmuster ohne eigene Dauer: Vorgabe; auf der Detailseite 15 min gewählt.
        assertNull(repo.loadForEdit(id)!!.minutes)
        overrides["muster-$id"] = 15

        val loaded = repo.loadForEdit(id)!!
        assertEquals(15, loaded.minutes)

        repo.update(id, daily.spec, loaded.minutes!!, name = "Nur umbenannt")
        assertEquals(15, dao.rows.value.single().minutes)
        assertTrue(overrides.isEmpty())
        assertEquals(15, repo.loadForEdit(id)!!.minutes)
    }
}
