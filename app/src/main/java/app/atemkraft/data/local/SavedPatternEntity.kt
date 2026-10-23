package app.atemkraft.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.domain.PatternOrigin
import app.atemkraft.domain.PatternSpec
import kotlinx.coroutines.flow.Flow

/**
 * Ein vom Nutzer gespeichertes Atemmuster (Parameter in Sekunden): ein gespeichertes Muster des
 * Tages oder ein im Editor zusammengestelltes eigenes Muster.
 *
 * [origin] ist die Herkunft als String ([ORIGIN_DAILY], [ORIGIN_CUSTOM]), nie das Enum direkt:
 * Unbekannte Werte fallen beim Lesen auf „eigenes Muster“ zurück ([patternOrigin]), wie bei
 * [LogEntryEntity]. [minutes] ist die Vorgabedauer eigener Muster (1–30), `null` = ≈ 5 min.
 * Beide Spalten gibt es seit Schema v4 (v5 gleicht nur frühe Entwicklungsstände aus); ältere
 * Zeilen sind gespeicherte Tagesmuster.
 *
 * [phases] ist die freie Phasenfolge eines eigenen Musters („Frei gestalten“) in der kompakten
 * Textform von [FreePattern.toCompact], z. B. „in:4,hold:1,out:6“; `null` bei gespeicherten
 * Tagesmustern, die bei den vier Feldern bleiben. Bei einer Zeile mit Folge tragen die vier Felder
 * deren Zusammenfassung ([columnSpec]): für ältere App-Versionen (Backup) und als Rückfall, falls
 * die Folge einmal nicht lesbar ist. Maßgeblich ist immer die Folge. Auch seit Schema v4.
 */
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
    @ColumnInfo(defaultValue = ORIGIN_DAILY) val origin: String = ORIGIN_DAILY,
    val minutes: Int? = null,
    val phases: String? = null,
) {
    companion object {
        /** Gespeichertes Muster des Tages (auch alle Zeilen aus Schema v3). */
        const val ORIGIN_DAILY = "daily"

        /** Im Muster-Editor zusammengestellt („Eigenes Muster“). */
        const val ORIGIN_CUSTOM = "custom"

        /** Gespeicherte Herkunft → Enum; alles außer [ORIGIN_DAILY] gilt als eigenes Muster. */
        fun originOf(stored: String?): PatternOrigin = if (stored == ORIGIN_DAILY) PatternOrigin.SAVED_DAILY else PatternOrigin.CUSTOM

        /** Normalform für die Spalte: unbekannte Werte werden [ORIGIN_CUSTOM]. */
        fun storedOrigin(origin: PatternOrigin): String = if (origin == PatternOrigin.CUSTOM) ORIGIN_CUSTOM else ORIGIN_DAILY
    }
}

fun SavedPatternEntity.toSpec(): PatternSpec = PatternSpec(inhale, holdFull, exhale, holdEmpty, activating)

/**
 * Die freie Folge dieser Zeile; `null` bei gespeicherten Tagesmustern, ohne Folge oder wenn sie
 * nicht lesbar ist bzw. die Regeln nicht einhält ([FreePattern.parse] prüft beides). Dann gelten
 * die vier Felder ([toSpec]). Eine Folge zählt nur bei eigenen Mustern: Die App schreibt sie nie
 * zu einem Tagesmuster.
 */
fun SavedPatternEntity.freePattern(): FreePattern? = if (patternOrigin() == PatternOrigin.CUSTOM) FreePattern.parse(phases) else null

/**
 * Zusammenfassung einer freien Folge in den vier festen Feldern (Spalten bzw. Backup-Felder):
 * Ein und Aus als Summen, Halten ab halb voller Lunge als „Halten voll“, darunter als „Halten
 * leer“ (dieselbe Grenze wie [FreePattern.intensityReasons], [FreePattern.isUpperSide]), jeweils summiert, 0 s wird `null`.
 * [PatternSpec.activating] folgt der Einordnung: mehr Ein als Aus. Nur Ersatz – die Folge selbst
 * lässt sich daraus nicht zurückgewinnen.
 */
fun FreePattern.columnSpec(): PatternSpec {
    var holdFull = 0
    var holdEmpty = 0
    phases.forEachIndexed { i, phase ->
        if (phase.type == FreePhaseType.HOLD) {
            if (isUpperSide(i)) holdFull += phase.seconds else holdEmpty += phase.seconds
        }
    }
    return PatternSpec(
        inhale = inhaleSeconds.toDouble(),
        holdFull = holdFull.takeIf { it > 0 }?.toDouble(),
        exhale = exhaleSeconds.toDouble(),
        holdEmpty = holdEmpty.takeIf { it > 0 }?.toDouble(),
        activating = inhaleSeconds > exhaleSeconds,
    )
}

fun SavedPatternEntity.patternOrigin(): PatternOrigin = SavedPatternEntity.originOf(origin)

@Dao
interface SavedPatternDao {

    /** Liefert die neue Id (für die Anpassungen eines importierten Musters). */
    @Insert
    suspend fun insert(pattern: SavedPatternEntity): Long

    @Query("SELECT * FROM saved_patterns ORDER BY createdAtEpochMs DESC")
    fun all(): Flow<List<SavedPatternEntity>>

    /** Momentaufnahme für Export/Import. */
    @Query("SELECT * FROM saved_patterns ORDER BY createdAtEpochMs ASC")
    suspend fun snapshot(): List<SavedPatternEntity>

    @Query("SELECT * FROM saved_patterns WHERE id = :id")
    suspend fun byId(id: Long): SavedPatternEntity?

    /**
     * Überschreibt Werte, Folge ([SavedPatternEntity.phases], `null` = nur die vier Felder), Name
     * und Dauer eines Musters und macht es zum eigenen Muster.
     * `createdAtEpochMs` bleibt stehen: Es ist Teil der Import-Identität. Liefert die Zahl
     * geänderter Zeilen (0 = das Muster gibt es nicht mehr).
     */
    @Query(
        """
        UPDATE saved_patterns SET name = :name, inhale = :inhale, holdFull = :holdFull, exhale = :exhale,
            holdEmpty = :holdEmpty, activating = :activating, minutes = :minutes, phases = :phases,
            origin = 'custom'
        WHERE id = :id
        """,
    )
    suspend fun updateCustom(
        id: Long,
        name: String,
        inhale: Double,
        holdFull: Double?,
        exhale: Double,
        holdEmpty: Double?,
        activating: Boolean,
        minutes: Int?,
        phases: String?,
    ): Int

    @Query("DELETE FROM saved_patterns WHERE id = :id")
    suspend fun delete(id: Long)
}
