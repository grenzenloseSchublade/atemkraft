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

    @Query("SELECT * FROM log_entries ORDER BY startedAtEpochMs DESC")
    fun all(): Flow<List<LogEntryEntity>>

    @Query("SELECT COUNT(*) FROM log_entries")
    fun count(): Flow<Int>

    /** Befinden eines Eintrags setzen oder ändern ([feeling] = Enum-Name, `null` = keine Antwort). */
    @Query("UPDATE log_entries SET feeling = :feeling WHERE id = :id")
    suspend fun setFeeling(id: Long, feeling: String?)

    @Query("DELETE FROM log_entries")
    suspend fun clear()
}
