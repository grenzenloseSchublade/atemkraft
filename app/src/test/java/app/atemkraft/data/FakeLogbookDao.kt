package app.atemkraft.data

import app.atemkraft.data.local.LogEntryEntity
import app.atemkraft.data.local.LogbookDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Logbuch im Speicher für Ablauf- und Sicherungstests: Ids ab 1, Sortierung wie die Room-Abfragen. */
class FakeLogbookDao : LogbookDao {
    val rows = mutableListOf<LogEntryEntity>()

    override suspend fun insert(entry: LogEntryEntity): Long {
        rows += entry.copy(id = rows.size + 1L)
        return rows.size.toLong()
    }

    override suspend fun insertAll(entries: List<LogEntryEntity>) {
        entries.forEach { insert(it) }
    }

    override fun all(): Flow<List<LogEntryEntity>> = MutableStateFlow(rows.sortedByDescending { it.startedAtEpochMs })

    override suspend fun snapshot(): List<LogEntryEntity> = rows.sortedBy { it.startedAtEpochMs }

    override fun count(): Flow<Int> = MutableStateFlow(rows.size)

    override suspend fun setFeeling(id: Long, feeling: String?) {
        rows.replaceAll { if (it.id == id) it.copy(feeling = feeling) else it }
    }

    override suspend fun clear() = rows.clear()
}
