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

    /** Alle Einträge, älteste zuerst (Sicherung). */
    suspend fun snapshot(): List<SessionLogEntry> = dao.snapshot().map { it.toDomain() }

    /**
     * Übernimmt Einträge aus einer Sicherung ohne Dubletten: Ein Eintrag gilt als vorhanden, wenn Art,
     * Übung und Startzeit gleich sind. Gibt die Zahl der neuen zurück; ein vorhandener Eintrag behält
     * sein Befinden.
     */
    suspend fun merge(incoming: List<SessionLogEntry>): Int {
        val known = dao.snapshot().map { it.toDomain().identity() }.toMutableSet()
        val fresh = incoming.filter { known.add(it.identity()) }
        if (fresh.isNotEmpty()) dao.insertAll(fresh.map { it.copy(id = 0).toEntity() })
        return fresh.size
    }

    suspend fun clear() = dao.clear()

    private fun SessionLogEntry.identity() = "${kind.name}|$exerciseId|$startedAtEpochMs"
}
