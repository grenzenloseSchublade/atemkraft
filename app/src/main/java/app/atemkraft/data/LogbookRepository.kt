package app.atemkraft.data

import app.atemkraft.data.local.LogbookDao
import app.atemkraft.data.local.toDomain
import app.atemkraft.data.local.toEntity
import app.atemkraft.domain.SessionLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Logbuch-Persistenz über Room. Liefert Domain-Einträge, neueste zuerst. */
class LogbookRepository(private val dao: LogbookDao) {

    val entries: Flow<List<SessionLogEntry>> = dao.all().map { list -> list.map { it.toDomain() } }

    /** Zahl der Einträge (Einstellungen, Zeile „Logbuch“): zählt in der DB, statt alle Zeilen zu laden (A-16). */
    val count: Flow<Int> = dao.count()

    suspend fun append(entry: SessionLogEntry) = dao.insert(entry.toEntity())

    suspend fun clear() = dao.clear()
}
