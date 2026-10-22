package app.atemkraft.data

import app.atemkraft.data.local.SavedPatternDao
import app.atemkraft.data.local.SavedPatternEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** DAO im Speicher für Repository-Tests: gleiche Sortierung wie die Room-Abfragen. */
open class FakeSavedPatternDao : SavedPatternDao {
    val rows = MutableStateFlow<List<SavedPatternEntity>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(pattern: SavedPatternEntity): Long {
        val id = nextId++
        rows.value = rows.value + pattern.copy(id = id)
        return id
    }

    override fun all(): Flow<List<SavedPatternEntity>> = rows.map { list -> list.sortedByDescending { it.createdAtEpochMs } }

    override suspend fun snapshot(): List<SavedPatternEntity> = rows.value.sortedBy { it.createdAtEpochMs }

    override suspend fun byId(id: Long): SavedPatternEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun updateCustom(
        id: Long,
        name: String,
        inhale: Double,
        holdFull: Double?,
        exhale: Double,
        holdEmpty: Double?,
        activating: Boolean,
        minutes: Int?,
        phases: String?,
    ): Int {
        if (rows.value.none { it.id == id }) return 0
        rows.value = rows.value.map {
            if (it.id != id) {
                it
            } else {
                it.copy(
                    name = name,
                    inhale = inhale,
                    holdFull = holdFull,
                    exhale = exhale,
                    holdEmpty = holdEmpty,
                    activating = activating,
                    minutes = minutes,
                    phases = phases,
                    origin = SavedPatternEntity.ORIGIN_CUSTOM,
                )
            }
        }
        return 1
    }

    override suspend fun delete(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
