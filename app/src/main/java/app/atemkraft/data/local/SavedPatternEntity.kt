package app.atemkraft.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import app.atemkraft.domain.PatternSpec
import kotlinx.coroutines.flow.Flow

/** Ein vom Nutzer gespeichertes generiertes Atemmuster (Parameter in Sekunden). */
@Entity(tableName = "saved_patterns")
data class SavedPatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val inhale: Double,
    val holdFull: Double?,
    val exhale: Double,
    val holdEmpty: Double?,
    val activating: Boolean,
    val createdAtEpochMs: Long,
)

fun SavedPatternEntity.toSpec(): PatternSpec = PatternSpec(inhale, holdFull, exhale, holdEmpty, activating)

@Dao
interface SavedPatternDao {

    @Insert
    suspend fun insert(pattern: SavedPatternEntity)

    @Query("SELECT * FROM saved_patterns ORDER BY createdAtEpochMs DESC")
    fun all(): Flow<List<SavedPatternEntity>>

    @Query("DELETE FROM saved_patterns WHERE id = :id")
    suspend fun delete(id: Long)
}
