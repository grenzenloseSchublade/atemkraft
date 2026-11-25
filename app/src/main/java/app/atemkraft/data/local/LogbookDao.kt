package app.atemkraft.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogbookDao {

    /** Neuer Eintrag; gibt seine Id zurück (für das Befinden, das der Ende-Screen danach setzt). */
    @Insert
    suspend fun insert(entry: LogEntryEntity): Long

    /** Einträge aus einer Sicherung (schon ohne Dubletten). */
    @Insert
    suspend fun insertAll(entries: List<LogEntryEntity>)

    @Query("SELECT * FROM log_entries ORDER BY startedAtEpochMs DESC")
    fun all(): Flow<List<LogEntryEntity>>

    /** Alle Einträge auf einmal, älteste zuerst (Sicherung, Abgleich beim Einlesen). */
    @Query("SELECT * FROM log_entries ORDER BY startedAtEpochMs")
    suspend fun snapshot(): List<LogEntryEntity>

    @Query("SELECT COUNT(*) FROM log_entries")
    fun count(): Flow<Int>

    /** Befinden eines Eintrags setzen oder ändern ([feeling] = Enum-Name, `null` = keine Antwort). */
    @Query("UPDATE log_entries SET feeling = :feeling WHERE id = :id")
    suspend fun setFeeling(id: Long, feeling: String?)

    /**
     * Stille nach der Atemübung an ihren Eintrag hängen („Atmen → Stille“ ist ein Eintrag): Dauer
     * und Stille wachsen um [silenceMs]. Gibt die Zahl geänderter Zeilen zurück (0 = Eintrag weg).
     */
    @Query("UPDATE log_entries SET durationMs = durationMs + :silenceMs, silenceMs = IFNULL(silenceMs, 0) + :silenceMs WHERE id = :id")
    suspend fun addSilence(id: Long, silenceMs: Long): Int

    @Query("DELETE FROM log_entries")
    suspend fun clear()
}
