package app.atemkraft.data

import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.PatternOrigin
import app.atemkraft.domain.RandomPatternGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Vorherige Fassung, Duplizieren und Aufräumen gespeicherter Muster (MUSTER-14, SEC-PRIV-04): Das
 * erste Auto-Speichern einer Sitzung legt den Stand beim Öffnen ab, spätere lassen ihn stehen;
 * Wiederherstellen tauscht (umkehrbar) und verliert keine auf der Detailseite gewählte Dauer;
 * Bearbeiten löscht nur die Anpassungen des Musters, nicht „Danach Stille“; die Kopie startet ohne
 * Vergangenheit; Löschen und das Abwählen des Tagesmusters nehmen Anpassungen und
 * Sicherheits-Bestätigungen mit, und die Sicherung trägt keine vorherige Fassung.
 */
class SavedPatternVersionsTest {

    private val box = FreePatternTemplates.all.single { it.label == "Box" }.pattern
    private val viloma = FreePatternTemplates.all.single { it.label == "Viloma I" }.pattern
    private val sigh = FreePatternTemplates.all.single { it.label == "4-7-8" }.pattern

    @Test
    fun `Auto-Speichern legt die vorherige Fassung einmal je Sitzung ab, Wiederherstellen tauscht`() = runBlocking {
        val cleared = mutableListOf<String>()
        val repo = SavedPatternsRepository(FakeSavedPatternDao(), clearOverrides = { error("löscht auch die Stille") }, clearPatternOverrides = { cleared += it })
        val id = repo.saveCustom(box, minutes = 5, name = "Abendwelle").id
        val opened = PatternVersion(box, 5, "Abendwelle")

        // Sitzung: erstes Schreiben mit dem Öffnungsstand, zweites ohne – die Fassung bleibt der Öffnungsstand.
        repo.update(id, viloma, 7, "Abendwelle", previous = opened)
        val afterSecond = repo.update(id, sigh, 7, "Stufen", previous = null)!!
        assertEquals(opened, afterSecond.previous)
        assertTrue(afterSecond.canRestore)
        assertEquals(listOf("muster-$id", "muster-$id"), cleared)

        // Wiederherstellen tauscht – ein zweites Mal holt den Stand von eben zurück.
        val restored = repo.restorePrevious(id)!!
        assertEquals(opened, restored.version)
        assertEquals(PatternVersion(sigh, 7, "Stufen"), restored.previous)
        assertEquals(PatternVersion(sigh, 7, "Stufen"), repo.restorePrevious(id)!!.version)

        // Gleicht der Stand der Fassung, gibt es nichts wiederherzustellen.
        assertFalse(repo.update(id, box, 5, "Abendwelle")!!.canRestore)

        // Die Sicherung trägt keine vorherige Fassung (nur lokale Rücknahme).
        val json = PatternBackup.encode(repo.exportFile(nowEpochMs = 0) { null })
        assertFalse(json.contains("previous"))
    }

    @Test
    fun `Kopie ohne Vergangenheit, Loeschen raeumt Anpassungen und Bestaetigungen auf`() = runBlocking {
        val cleared = mutableListOf<String>()
        val forgotten = mutableListOf<String>()
        val repo = SavedPatternsRepository(
            FakeSavedPatternDao(),
            clearOverrides = { cleared += it },
            clearPatternOverrides = {},
            overrideMinutes = { 12 },
            forgetSafety = { forgotten += it },
        )
        val long = "A".repeat(PatternBackup.MAX_NAME)
        val id = repo.saveCustom(box, minutes = 5, name = long).id
        repo.update(id, viloma, 5, long, previous = PatternVersion(box, 5, long))

        val copy = repo.duplicate(id) { "$it (Kopie)" }!!
        assertTrue(copy.id != id)
        assertEquals(PatternOrigin.CUSTOM, copy.origin)
        assertEquals(viloma, copy.pattern)
        // Die Dauer, die die Detailseite zeigt (dort gespeicherte Anpassung), wird die Dauer der Kopie.
        assertEquals(12, copy.minutes)
        assertNull(copy.previous)
        assertTrue(copy.exercise.name.endsWith(" (Kopie)"))
        assertTrue(copy.exercise.name.length <= PatternBackup.MAX_NAME)

        // Wiederherstellen legt die Dauer ab, die die Detailseite zeigt (12), nicht die Spalte (5).
        assertEquals(PatternVersion(viloma, 12, long), repo.restorePrevious(id)!!.previous)

        cleared.clear()
        repo.delete(id)
        assertEquals(listOf("muster-$id"), cleared)
        assertEquals(listOf("muster-$id"), forgotten)

        // Das Tagesmuster abwählen räumt genauso auf.
        val daily = RandomPatternGenerator.forSeed(7)
        repo.save(daily)
        val dailyId = repo.patterns.first().single { it.origin == PatternOrigin.SAVED_DAILY }.id
        repo.unsave(daily)
        assertEquals("muster-$dailyId", cleared.last())
        assertEquals("muster-$dailyId", forgotten.last())
    }
}
