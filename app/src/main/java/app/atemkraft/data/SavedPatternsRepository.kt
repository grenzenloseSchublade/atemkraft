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

    /** Speichert das Muster; identischer Name wird nicht doppelt angelegt. */
    suspend fun save(daily: DailyPattern) {
        val name = savedName(daily)
        if (cached.any { it.exercise.name == name }) return
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

    suspend fun delete(id: Long) = dao.delete(id)

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
