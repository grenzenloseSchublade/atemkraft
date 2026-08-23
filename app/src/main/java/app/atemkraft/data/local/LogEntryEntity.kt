package app.atemkraft.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.SessionKind
import app.atemkraft.domain.SessionLogEntry

/**
 * Room-Persistenzform eines Logbuch-Eintrags. Familie als Enum-Name gespeichert (bei
 * Meditation `null`); [kind] unterscheidet Atemübung von Meditation.
 */
@Entity(tableName = "log_entries")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val exerciseName: String,
    val family: String?,
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val roundsCompleted: Int,
    val kind: String = SessionKind.BREATHING.name,
)

fun LogEntryEntity.toDomain(): SessionLogEntry = SessionLogEntry(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    // Unbekannte Familie (z. B. nach künftiger Enum-Umbenennung) darf nicht den ganzen
    // Logbuch-Flow crashen – wie bei kind defensiv behandeln.
    family = family?.let { runCatching { BreathingFamily.valueOf(it) }.getOrNull() },
    startedAtEpochMs = startedAtEpochMs,
    durationMs = durationMs,
    roundsCompleted = roundsCompleted,
    kind = runCatching { SessionKind.valueOf(kind) }.getOrDefault(SessionKind.BREATHING),
)

fun SessionLogEntry.toEntity(): LogEntryEntity = LogEntryEntity(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    family = family?.name,
    startedAtEpochMs = startedAtEpochMs,
    durationMs = durationMs,
    roundsCompleted = roundsCompleted,
    kind = kind.name,
)
