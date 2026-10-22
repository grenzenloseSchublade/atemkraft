package app.atemkraft.data

import app.atemkraft.data.local.SavedPatternDao
import app.atemkraft.data.local.SavedPatternEntity
import app.atemkraft.data.local.patternOrigin
import app.atemkraft.data.local.toSpec
import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.DailyPattern
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PatternOrigin
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.withinCreationRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Ergebnis eines Imports: neu angelegt und als schon vorhanden übersprungen. */
data class ImportResult(val added: Int, val duplicates: Int)

/**
 * Ein gespeichertes Muster fürs UI: DB-Id + fertige Übung + Parameter, dazu Herkunft und
 * Vorgabedauer ([minutes], `null` = ≈ 5 min).
 */
data class SavedPattern(
    val id: Long,
    val exercise: Exercise,
    val spec: PatternSpec,
    val origin: PatternOrigin = PatternOrigin.SAVED_DAILY,
    val minutes: Int? = null,
) {
    val activating: Boolean get() = spec.activating
}

/**
 * Vom Nutzer gespeicherte Atemmuster (Room): gespeicherte Muster des Tages und eigene Muster aus
 * dem Editor. Hält zusätzlich einen synchronen Cache der aktuellen Liste, damit
 * [ExerciseRepository.byId] gespeicherte Übungen ohne Suspend-Aufruf auflösen kann
 * (Session-Start ist synchron).
 *
 * Der Cache folgt der DB, nimmt eigene Schreibvorgänge aber sofort auf: Ein gerade gespeichertes
 * oder geändertes Muster ist direkt danach auflösbar, und ein später eintreffender älterer
 * DB-Stand überschreibt es nicht, bis die DB den Schreibvorgang selbst liefert.
 *
 * @param clearOverrides löscht die Detailseiten-Anpassungen einer Übung (DataStore); [update]
 *   ruft es auf, sonst überstimmen alte Anpassungen die neuen Werte.
 */
class SavedPatternsRepository(
    private val dao: SavedPatternDao,
    private val clearOverrides: suspend (exerciseId: String) -> Unit = {},
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    @Volatile
    private var cached: List<SavedPattern> = emptyList()

    /** Zuletzt von der DB gelieferter Stand und eigene, noch nicht bestätigte Schreibvorgänge. */
    private val cacheLock = Any()
    private var dbRows: List<SavedPatternEntity> = emptyList()
    private val pendingWrites = mutableMapOf<Long, SavedPatternEntity>()
    private val pendingDeletes = mutableSetOf<Long>()

    /** Aktuelle Liste, neueste zuerst (für die Liste „Meine Muster“). */
    val patterns: Flow<List<SavedPattern>> = dao.all().map { list -> list.map { it.toSaved() } }

    init {
        scope.launch { dao.all().collect { onDbRows(it) } }
    }

    /** Anzeigename, unter dem dieses Tagesmuster gespeichert wird/wurde. */
    fun savedName(daily: DailyPattern): String = daily.exercise.name.replace("Tagesmuster", "Muster")

    /**
     * Ist [daily] in [patterns] als Tagesmuster gespeichert? Vergleicht Name und Herkunft: Ein
     * eigenes Muster mit gleichen Zahlen (und gleichem Namen) zählt nicht.
     */
    fun isDailySaved(daily: DailyPattern, patterns: List<SavedPattern>): Boolean {
        val name = savedName(daily)
        return patterns.any { it.origin == PatternOrigin.SAVED_DAILY && it.exercise.name == name }
    }

    /**
     * Schreibvorgänge nacheinander abarbeiten: Das Lesezeichen zeigt den Zustand erst, wenn
     * die DB ihn liefert. Ein schneller Doppeltipp löst deshalb zweimal „speichern“ aus, und
     * Speichern/Entfernen dürfen sich nicht überholen.
     */
    private val writeLock = Mutex()

    /**
     * Speichert das Tagesmuster; ein gespeichertes Tagesmuster gleichen Namens wird nicht doppelt
     * angelegt (eigene Muster zählen nicht). Geprüft wird in der DB, nicht im Cache: Der hinkt
     * direkt nach dem vorigen Speichern noch hinterher.
     */
    suspend fun save(daily: DailyPattern) {
        writeLock.withLock {
            val name = savedName(daily)
            if (dao.snapshot().any { it.isDaily() && it.name == name }) return
            insertAndCache(entityOf(daily.spec, name, SavedPatternEntity.ORIGIN_DAILY, minutes = null))
        }
    }

    /**
     * Legt ein im Editor zusammengestelltes Muster an. [spec] muss die Erzeugungsregeln
     * einhalten, [minutes] in 1–30 liegen. Ein leerer [name] wird zum Vorschlagsnamen
     * („Eigenes Muster 4·1·6“, siehe [nameFor]). Das Muster ist sofort per
     * [byExerciseId] auflösbar.
     */
    suspend fun saveCustom(spec: PatternSpec, minutes: Int, name: String): SavedPattern {
        requireValid(spec, minutes)
        return writeLock.withLock {
            val entity = entityOf(spec, nameFor(spec, name), SavedPatternEntity.ORIGIN_CUSTOM, minutes)
            insertAndCache(entity).toSaved()
        }
    }

    /**
     * Schreibt neue Werte, Dauer und Namen in dasselbe Muster (gleiche Id, gleiches
     * Anlagedatum) und macht es zum eigenen Muster. Löscht die Detailseiten-Anpassungen des
     * Musters. `null`, wenn es das Muster nicht mehr gibt.
     */
    suspend fun update(id: Long, spec: PatternSpec, minutes: Int, name: String): SavedPattern? {
        requireValid(spec, minutes)
        val updated = writeLock.withLock {
            val finalName = nameFor(spec, name)
            val changed = dao.updateCustom(
                id = id,
                name = finalName,
                inhale = spec.inhale,
                holdFull = spec.holdFull,
                exhale = spec.exhale,
                holdEmpty = spec.holdEmpty,
                activating = spec.activating,
                minutes = minutes,
            )
            if (changed == 0) return@withLock null
            val entity = dao.byId(id) ?: return@withLock null
            remember(entity)
            entity
        } ?: return null
        clearOverrides("$ID_PREFIX$id")
        return updated.toSaved()
    }

    /** Das Muster mit DB-Id [id] für den Editor (Bearbeiten-Modus); null, wenn es fehlt. */
    suspend fun loadForEdit(id: Long): SavedPattern? = dao.byId(id)?.toSaved()

    /**
     * Die Übung zu [exerciseId] („muster-7“), solange es sie gibt: Die Detailseite zeigt so nach
     * dem Bearbeiten sofort die neuen Werte. Der erste Wert kommt aus der DB; `null` heißt, das
     * Muster gibt es nicht (mehr).
     */
    fun observe(exerciseId: String): Flow<Exercise?> {
        val id = idOf(exerciseId) ?: return flowOf(null)
        return patterns.map { list -> list.firstOrNull { it.id == id }?.exercise }.distinctUntilChanged()
    }

    suspend fun delete(id: Long) {
        writeLock.withLock {
            dao.delete(id)
            forget(id)
        }
    }

    /**
     * Nimmt das Tagesmuster wieder heraus (Lesezeichen abgewählt). Gesucht wird per Name und
     * Herkunft in der DB, nicht im Cache: Der hinkt direkt nach dem Speichern noch hinterher.
     * Ein eigenes Muster mit gleichem Namen bleibt stehen.
     */
    suspend fun unsave(daily: DailyPattern) {
        writeLock.withLock {
            val name = savedName(daily)
            dao.snapshot().filter { it.isDaily() && it.name == name }.forEach {
                dao.delete(it.id)
                forget(it.id)
            }
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
                origin = SavedPatternEntity.storedOrigin(e.patternOrigin()),
                minutes = e.minutes,
            )
        },
    )

    /**
     * Legt die Einträge an, die es noch nicht gibt (gleiche Herkunft und Werte = Dublette, auch
     * innerhalb der Datei). Anpassungen landen unter der neuen Id des Musters. Fehlt die
     * Herkunft (Dateien bis 1.6.8), ist es ein Tagesmuster; ein unbekannter Wert wird „custom“.
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
            val entity = writeLock.withLock {
                insertAndCache(
                    SavedPatternEntity(
                        name = entry.name.trim(),
                        inhale = entry.inhale,
                        holdFull = entry.holdFull,
                        exhale = entry.exhale,
                        holdEmpty = entry.holdEmpty,
                        activating = entry.activating,
                        createdAtEpochMs = entry.createdAtEpochMs,
                        origin = SavedPatternEntity.storedOrigin(
                            SavedPatternEntity.originOf(entry.origin ?: SavedPatternEntity.ORIGIN_DAILY),
                        ),
                        minutes = entry.minutes,
                    ),
                )
            }
            entry.overrides?.let { writeOverrides("$ID_PREFIX${entity.id}", it) }
            added++
        }
        return ImportResult(added = added, duplicates = entries.size - added)
    }

    /** Synchrone Auflösung für den Session-Start ([ExerciseRepository.byId]). */
    fun byExerciseId(exerciseId: String): Exercise? {
        val id = idOf(exerciseId) ?: return null
        return cached.firstOrNull { it.id == id }?.exercise
    }

    /**
     * Name eines eigenen Musters: die bereinigte Eingabe ([PatternBackup.cleanName], höchstens
     * 80 Zeichen), bei leerer Eingabe der Vorschlag „Eigenes Muster 4·1·6“.
     */
    fun nameFor(spec: PatternSpec, input: String): String = PatternBackup.cleanName(input).ifEmpty { suggestedName(spec) }

    private fun requireValid(spec: PatternSpec, minutes: Int) {
        require(spec.withinCreationRules()) { "Muster außerhalb der Erzeugungsregeln: $spec" }
        require(minutes in AdjustLimits.MINUTES) { "Dauer außerhalb 1–30 min: $minutes" }
    }

    private suspend fun insertAndCache(entity: SavedPatternEntity): SavedPatternEntity {
        val stored = entity.copy(id = dao.insert(entity))
        remember(stored)
        return stored
    }

    /** Eigener Schreibvorgang: sofort in den Cache, bis die DB ihn bestätigt. */
    private fun remember(entity: SavedPatternEntity) = synchronized(cacheLock) {
        pendingDeletes.remove(entity.id)
        pendingWrites[entity.id] = entity
        rebuildCache()
    }

    private fun forget(id: Long) = synchronized(cacheLock) {
        pendingWrites.remove(id)
        pendingDeletes.add(id)
        rebuildCache()
    }

    /**
     * Neuer DB-Stand. Eigene Schreibvorgänge gelten als bestätigt, sobald die DB sie genau so
     * liefert; bis dahin überstimmen sie den (womöglich älteren) DB-Stand.
     */
    private fun onDbRows(rows: List<SavedPatternEntity>) = synchronized(cacheLock) {
        dbRows = rows
        pendingWrites.values.removeAll { pending -> pending in rows }
        pendingDeletes.removeAll { id -> rows.none { it.id == id } }
        rebuildCache()
    }

    private fun rebuildCache() {
        val merged = dbRows.filterNot { it.id in pendingDeletes || it.id in pendingWrites } + pendingWrites.values
        cached = merged.sortedByDescending { it.createdAtEpochMs }.map { it.toSaved() }
    }

    private fun SavedPatternEntity.isDaily(): Boolean = patternOrigin() == PatternOrigin.SAVED_DAILY

    private fun SavedPatternEntity.toSaved(): SavedPattern {
        val spec = toSpec()
        val origin = patternOrigin()
        return SavedPattern(
            id = id,
            exercise = RandomPatternGenerator.exerciseFrom(spec, "$ID_PREFIX$id", name, origin, minutes),
            spec = spec,
            origin = origin,
            minutes = minutes,
        )
    }

    companion object {
        /** Übungs-Id-Präfix gespeicherter Muster (z. B. „muster-7“). */
        const val ID_PREFIX = "muster-"

        /** Vorschlagsname eines eigenen Musters, z. B. „Eigenes Muster 4·1·6“. */
        fun suggestedName(spec: PatternSpec): String = "Eigenes Muster ${RandomPatternGenerator.numbersOf(spec)}"

        /** DB-Id aus einer Übungs-Id („muster-7“ → 7); null für andere Übungen. */
        fun idOf(exerciseId: String): Long? = exerciseId.takeIf { it.startsWith(ID_PREFIX) }?.removePrefix(ID_PREFIX)?.toLongOrNull()

        private fun entityOf(spec: PatternSpec, name: String, origin: String, minutes: Int?) = SavedPatternEntity(
            name = name,
            inhale = spec.inhale,
            holdFull = spec.holdFull,
            exhale = spec.exhale,
            holdEmpty = spec.holdEmpty,
            activating = spec.activating,
            createdAtEpochMs = System.currentTimeMillis(),
            origin = origin,
            minutes = minutes,
        )
    }
}
