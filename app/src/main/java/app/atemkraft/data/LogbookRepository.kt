package app.atemkraft.data

import app.atemkraft.data.local.LogbookDao
import app.atemkraft.data.local.toDomain
import app.atemkraft.data.local.toEntity
import app.atemkraft.domain.Feeling
import app.atemkraft.domain.SessionLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Logbuch-Persistenz über Room. Liefert Domain-Einträge, neueste zuerst. */
class LogbookRepository(private val dao: LogbookDao) {

    val entries: Flow<List<SessionLogEntry>> = dao.all().map { list -> list.map { it.toDomain() } }

    /** Zahl der Einträge (Einstellungen, Zeile „Logbuch“): zählt in der DB, statt alle Zeilen zu laden. */
    val count: Flow<Int> = dao.count()

    /** Schreibt den Eintrag und gibt seine Id zurück (für ein Befinden, das später kommt). */
    suspend fun append(entry: SessionLogEntry): Long = dao.insert(entry.toEntity())

    /** Befinden danach (SEC-LEGAL-04): steht nur am Eintrag, die App rechnet nie damit. */
    suspend fun setFeeling(id: Long, feeling: Feeling?) = dao.setFeeling(id, feeling?.name)

    suspend fun clear() = dao.clear()
}
