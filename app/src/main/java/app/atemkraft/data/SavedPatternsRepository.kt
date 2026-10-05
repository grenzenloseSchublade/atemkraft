package app.atemkraft.data

import app.atemkraft.data.local.SavedPatternDao
import app.atemkraft.data.local.SavedPatternEntity
import app.atemkraft.data.local.toSpec
import app.atemkraft.domain.DailyPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.RandomPatternGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Ergebnis eines Imports: neu angelegt und als schon vorhanden übersprungen. */
data class ImportResult(val added: Int, val duplicates: Int)

/** Ein gespeichertes Muster fürs UI: DB-Id + fertige Übung + Parameter. */
data class SavedPattern(val id: Long, val exercise: Exercise, val spec: PatternSpec) {
    val activating: Boolean get() = spec.activating
}

/**
 * Vom Nutzer gespeicherte generierte Atemmuster (Room). Hält zusätzlich einen synchronen
 * Cache der aktuellen Liste, damit [ExerciseRepository.byId] gespeicherte Übungen ohne
 * Suspend-Aufruf auflösen kann (Session-Start ist synchron).
 */
class SavedPatternsRepository(private val dao: SavedPatternDao) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cached: List<SavedPattern> = emptyList()

    /** Aktuelle Liste, neueste zuerst (für den „Meine Muster"-Abschnitt). */
    val patterns: Flow<List<SavedPattern>> = dao.all().map { list -> list.map { it.toSaved() } }

    init {
        scope.launch { patterns.collect { cached = it } }
    }

    /** Anzeigename, unter dem dieses Tagesmuster gespeichert wird/wurde. */
    fun savedName(daily: DailyPattern): String = daily.exercise.name.replace("Tagesmuster", "Muster")

    /**
     * Lesezeichen-Tipps nacheinander abarbeiten: Das Lesezeichen zeigt den Zustand erst, wenn
     * die DB ihn liefert. Ein schneller Doppeltipp löst deshalb zweimal „speichern“ aus, und
     * Speichern/Entfernen dürfen sich nicht überholen.
     */
    private val dailyLock = Mutex()

    /**
     * Speichert das Muster; identischer Name wird nicht doppelt angelegt. Geprüft wird in der
     * DB, nicht im Cache: Der hinkt direkt nach dem vorigen Speichern noch hinterher.
     */
    suspend fun save(daily: DailyPattern) {
        dailyLock.withLock {
            val name = savedName(daily)
            if (dao.snapshot().any { it.name == name }) return
            val spec = daily.spec
            dao.insert(
                SavedPatternEntity(
                    name = name,
                    inhale = spec.inhale,
                    holdFull = spec.holdFull,
                    exhale = spec.exhale,
                    holdEmpty = spec.holdEmpty,
                    activating = spec.activating,
                    createdAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    /**
     * Nimmt das Tagesmuster wieder heraus (Lesezeichen abgewählt). Gesucht wird per Name in der
     * DB, nicht im Cache: Der hinkt direkt nach dem Speichern noch hinterher.
     */
    suspend fun unsave(daily: DailyPattern) {
        dailyLock.withLock {
            val name = savedName(daily)
            dao.snapshot().filter { it.name == name }.forEach { dao.delete(it.id) }
        }
    }

    suspend fun isEmpty(): Boolean = dao.snapshot().isEmpty()

    /** Alle gespeicherten Muster samt Anpassung als Export-Datei (älteste zuerst). */
    suspend fun exportFile(
        nowEpochMs: Long,
        overridesOf: suspend (exerciseId: String) -> IntervalOverrides?,
    ): PatternBackupFile = PatternBackupFile(
        exportedAtEpochMs = nowEpochMs,
        patterns = dao.snapshot().map { e ->
            PatternBackupEntry(
                name = e.name,
                inhale = e.inhale,
                holdFull = e.holdFull,
                exhale = e.exhale,
                holdEmpty = e.holdEmpty,
                activating = e.activating,
                createdAtEpochMs = e.createdAtEpochMs,
                overrides = overridesOf("$ID_PREFIX${e.id}")?.let {
                    PatternBackupOverrides(it.duration, it.inhale, it.hold, it.exhale)
                },
            )
        },
    )

    /**
     * Legt die Einträge an, die es noch nicht gibt (gleiche Herkunft und Werte = Dublette, auch
     * innerhalb der Datei). Anpassungen landen unter der neuen Id des Musters.
     */
    suspend fun import(
        entries: List<PatternBackupEntry>,
        writeOverrides: suspend (exerciseId: String, overrides: PatternBackupOverrides) -> Unit,
    ): ImportResult {
        val known = dao.snapshot()
            .map { PatternBackup.identity(it.createdAtEpochMs, it.inhale, it.holdFull, it.exhale, it.holdEmpty, it.activating) }
            .toMutableSet()
        var added = 0
        for (entry in entries) {
            if (!known.add(with(PatternBackup) { entry.identity() })) continue
            val id = dao.insert(
                SavedPatternEntity(
                    name = entry.name.trim(),
                    inhale = entry.inhale,
                    holdFull = entry.holdFull,
                    exhale = entry.exhale,
                    holdEmpty = entry.holdEmpty,
                    activating = entry.activating,
                    createdAtEpochMs = entry.createdAtEpochMs,
                ),
            )
            entry.overrides?.let { writeOverrides("$ID_PREFIX$id", it) }
            added++
        }
        return ImportResult(added = added, duplicates = entries.size - added)
    }

    /** Synchrone Auflösung für den Session-Start ([ExerciseRepository.byId]). */
    fun byExerciseId(exerciseId: String): Exercise? {
        val id = exerciseId.removePrefix(ID_PREFIX).toLongOrNull() ?: return null
        return cached.firstOrNull { it.id == id }?.exercise
    }

    private fun SavedPatternEntity.toSaved(): SavedPattern {
        val spec = toSpec()
        return SavedPattern(
            id = id,
            exercise = RandomPatternGenerator.exerciseFrom(spec, "$ID_PREFIX$id", name),
            spec = spec,
        )
    }

    companion object {
        /** Übungs-Id-Präfix gespeicherter Muster (z. B. „muster-7"). */
        const val ID_PREFIX = "muster-"
    }
}
