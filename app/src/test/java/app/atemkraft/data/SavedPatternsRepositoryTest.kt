package app.atemkraft.data

import app.atemkraft.domain.RandomPatternGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

/** Lesezeichen am „Muster des Tages“: speichern und wieder herausnehmen über das Repository. */
class SavedPatternsRepositoryTest {

    private val daily = RandomPatternGenerator.forDate(LocalDate.of(2026, 10, 5))
    private val other = RandomPatternGenerator.forDate(LocalDate.of(2026, 10, 6))

    @Test
    fun `Doppeltipp auf das Lesezeichen legt das Tagesmuster nur einmal an`() = runBlocking {
        val dao = FakeSavedPatternDao()
        val repo = SavedPatternsRepository(dao)

        repo.save(daily)
        repo.save(daily)

        assertEquals(listOf(repo.savedName(daily)), dao.rows.value.map { it.name })
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
}
