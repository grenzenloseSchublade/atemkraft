package app.atemkraft.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogbookDao {

    @Insert
    suspend fun insert(entry: LogEntryEntity)

    @Query("SELECT * FROM log_entries ORDER BY startedAtEpochMs DESC")
    fun all(): Flow<List<LogEntryEntity>>

    @Query("DELETE FROM log_entries")
    suspend fun clear()
}
