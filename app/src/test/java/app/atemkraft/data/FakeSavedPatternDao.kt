package app.atemkraft.data

import app.atemkraft.data.local.SavedPatternDao
import app.atemkraft.data.local.SavedPatternEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** DAO im Speicher für Repository-Tests: gleiche Sortierung wie die Room-Abfragen. */
class FakeSavedPatternDao : SavedPatternDao {
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
