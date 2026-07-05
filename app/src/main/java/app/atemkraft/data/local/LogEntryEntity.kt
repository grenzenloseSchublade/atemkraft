package app.atemkraft.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.SessionLogEntry

/** Room-Persistenzform eines Logbuch-Eintrags. Familie als Enum-Name gespeichert. */
@Entity(tableName = "log_entries")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val exerciseName: String,
    val family: String,
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val roundsCompleted: Int,
)

fun LogEntryEntity.toDomain(): SessionLogEntry = SessionLogEntry(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    family = BreathingFamily.valueOf(family),
    startedAtEpochMs = startedAtEpochMs,
    durationMs = durationMs,
    roundsCompleted = roundsCompleted,
)

fun SessionLogEntry.toEntity(): LogEntryEntity = LogEntryEntity(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    family = family.name,
    startedAtEpochMs = startedAtEpochMs,
    durationMs = durationMs,
    roundsCompleted = roundsCompleted,
)
