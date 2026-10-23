package app.atemkraft.ui.patterns

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.data.PatternBackup
import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.domain.FreeTemplate
import app.atemkraft.domain.GeneratedExercises
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.toFreePattern

/**
 * Was gespeichert ist: Folge, Minuten und Name, so wie sie zuletzt geschrieben wurden. Das
 * Lesezeichen ist gefüllt, solange der Entwurf genau diesem Stand entspricht.
 */
data class PatternSnapshot(val pattern: FreePattern, val minutes: Int, val name: String)

/**
 * Zustand des Muster-Editors („Eigenes Muster“, MUSTER-10, Nachtrag 4 „Frei gestalten“), getrennt
 * von der Darstellung – nach dem Vorbild von `SessionAdjustState` (ARC-7). Kurve, Ablauf-Liste,
 * Hinzufügen-Knöpfe, Vorlagen, Name und Lesezeichen lesen und ändern nur diesen Zustand.
 *
 * **Invariante:** [pattern] ist immer eine gültige freie Folge ([FreePattern.isValid]): 2 bis 12
 * Phasen, jede 1 bis 30 s, mindestens ein Ein und ein Aus. Jede Änderung geht über die
 * Bearbeiten-Funktionen der Domain ([FreePattern.withSeconds], [FreePattern.inserted],
 * [FreePattern.removed]), die nur gültige Folgen liefern.
 *
 * - [selected] ist die gewählte Phase (Werkzeugleiste, Linie oder Griff der Kurve, Ablauf-Liste).
 *   **Es ist immer eine gewählt** (Nachtrag 5): beim Öffnen die letzte, nach dem Entfernen die
 *   Nachbarin, nach einer Vorlage oder dem Reset wieder die letzte. Neue Phasen kommen dahinter
 *   ([add]); die neue ist danach gewählt.
 * - „Startpunkt“ zeigt die Vorlage, der die Folge gerade entspricht ([template], Wertvergleich),
 *   sonst „angepasst“. [applyTemplate] setzt die Folge und wählt ihre letzte Phase.
 * - [reset] setzt die Folge auf den Stand beim Öffnen ([initial]) zurück, die Minuten bleiben.
 * - Der Name folgt dem Vorschlagsnamen der aktuellen Zahlen („Eigenes Muster 4·1·6“), bis die
 *   Person selbst tippt ([updateName], höchstens [NAME_MAX] Zeichen). Leer gespeichert heißt
 *   Vorschlagsname ([savedName]).
 * - [saved]: Es gibt einen gespeicherten Eintrag ([savedId]) und der Entwurf entspricht ihm
 *   ([snapshot]). Jede Änderung an Folge, Minuten oder Name leert das Lesezeichen.
 *
 * [namePrefix] kommt aus den Ressourcen (`pattern_editor_title`), weil dieser Zustand keine
 * Texte kennen soll.
 */
@Stable
class PatternEditorState internal constructor(
    val namePrefix: String,
    pattern: FreePattern,
    val initial: FreePattern,
    selected: Int,
    minutes: Int,
    typedName: String,
    nameEdited: Boolean,
    keptName: String,
    keptFor: FreePattern,
    savedId: Long?,
    snapshot: PatternSnapshot?,
    existedBefore: Boolean,
) {
    /** Die eingestellte Folge; immer gültig. */
    var pattern: FreePattern by mutableStateOf(pattern)
        private set

    private var selectedValue: Int by mutableIntStateOf(selected.takeIf { it in pattern.phases.indices } ?: pattern.phases.lastIndex)

    /** Die gewählte Phase (Index in [pattern]); es ist immer eine gewählt. */
    val selected: Int get() = selectedValue

    private var minutesValue: Int by mutableIntStateOf(minutes.coerceIn(AdjustLimits.MINUTES))

    /** Dauer in Minuten (1–30, geklemmt); ändert die Folge nie. */
    var minutes: Int
        get() = minutesValue
        set(value) {
            minutesValue = value.coerceIn(AdjustLimits.MINUTES)
        }

    private var typedName: String by mutableStateOf(typedName.take(NAME_MAX))

    /**
     * Ein gespeicherter Name, der den Zahlen folgt („Tagesmuster 4·2·6“), gilt weiter, solange
     * die Folge [keptFor] entspricht; danach greift der Vorschlagsname. Leer = keiner.
     */
    private val keptName: String = keptName
    private val keptFor: FreePattern = keptFor

    /** Hat die Person den Namen selbst getippt? Sonst folgt er den Zahlen. */
    var nameEdited: Boolean by mutableStateOf(nameEdited)
        private set

    /** Id des gespeicherten Eintrags in „Meine Muster“; null = noch nicht gespeichert. */
    var savedId: Long? by mutableStateOf(savedId)
        private set

    /** Stand des gespeicherten Eintrags; null, solange nichts gespeichert ist. */
    var snapshot: PatternSnapshot? by mutableStateOf(snapshot)
        private set

    /**
     * Gab es den Eintrag schon vor dem Öffnen (Bearbeiten-Modus)? Dann entfernt das Lesezeichen
     * ihn nur zweistufig (MUSTER-05); ein in dieser Sitzung angelegtes Muster nimmt ein erneuter
     * Tipp sofort wieder heraus.
     */
    var existedBefore: Boolean by mutableStateOf(existedBefore)
        private set

    /** Die Vorlage, der die Folge gerade genau entspricht; null heißt „angepasst“. */
    val template: FreeTemplate? get() = FreePatternTemplates.matching(pattern)

    /** Weicht die Folge vom Stand beim Öffnen ab (dann zeigt der Start den Reset)? */
    val modified: Boolean get() = pattern != initial

    /** Ist noch Platz für eine weitere Phase (höchstens 12)? */
    val canAdd: Boolean get() = pattern.canAdd

    /** Vorschlagsname der aktuellen Zahlen, z. B. „Eigenes Muster 4·1·6“. */
    val suggestedName: String get() = suggestedName(namePrefix, pattern)

    /**
     * Inhalt des Namensfelds: getippt, sonst der Vorschlagsname – beim Bearbeiten bis zur ersten
     * Änderung der Folge noch der gespeicherte Name.
     */
    val name: String
        get() = when {
            nameEdited -> typedName
            keptName.isNotEmpty() && pattern == keptFor -> keptName
            else -> suggestedName
        }

    /**
     * Der Name, unter dem gespeichert wird – so bereinigt wie im Repository ([PatternBackup.cleanName]),
     * damit das Lesezeichen nach dem Speichern zum gespeicherten Eintrag passt; leer → Vorschlagsname.
     */
    val savedName: String get() = PatternBackup.cleanName(name).ifEmpty { suggestedName }

    /** Entspricht der Entwurf dem gespeicherten Eintrag (Lesezeichen gefüllt)? */
    val saved: Boolean get() = savedId != null && snapshot == current()

    private val exercise: Exercise by derivedStateOf {
        GeneratedExercises.exerciseFrom(
            pattern = this.pattern,
            id = this.savedId?.let { "muster-$it" } ?: RandomPatternGenerator.DRAFT_ID,
            name = savedName,
            minutes = this.minutes,
        )
    }

    /**
     * Der Entwurf als lauffähige Übung (Dauer [minutes]): für „Starten“ ohne Speichern, den Ton
     * der Vorschau und die Live-Einordnung (`lore`). Ein intensives Muster trägt `caution` und
     * seine Sicherheitshinweise.
     */
    fun exercise(): Exercise = exercise

    /** Die Anpassung für den Start: nur die Dauer, die Phasen stecken schon in [exercise]. */
    fun config(): SessionConfig = SessionConfig(minutes = minutes)

    /** Phase [index] wählen (Werkzeugleiste, Kurve, Ablauf-Liste); außerhalb der Folge bleibt die Auswahl. */
    fun select(index: Int) {
        if (index in pattern.phases.indices) selectedValue = index
    }

    /** Phase [index] auf [seconds] setzen, geklemmt auf 1–30 s. */
    fun setSeconds(index: Int, seconds: Int) {
        pattern = pattern.withSeconds(index, seconds)
    }

    /**
     * Neue Phase vom Typ [type] hinter der gewählten, Ein und Aus mit 4 s, Halten mit 2 s; die
     * neue ist danach gewählt. Bei 12 Phasen geschieht nichts.
     */
    fun add(type: FreePhaseType) {
        if (!pattern.canAdd) return
        val at = pattern.insertIndex(selectedValue)
        pattern = pattern.inserted(selectedValue, type)
        selectedValue = at
    }

    /** Darf Phase [index] weg, ohne dass die Folge ungültig wird? */
    fun canRemove(index: Int): Boolean = pattern.canRemove(index)

    /**
     * Entfernt Phase [index], wenn die Folge danach gültig bleibt; gewählt ist dann die Phase an
     * ihrer Stelle (bzw. die neue letzte), wie in der Vorlage.
     */
    fun remove(index: Int) {
        if (!pattern.canRemove(index)) return
        pattern = pattern.removed(index)
        selectedValue = index.coerceAtMost(pattern.phases.lastIndex)
    }

    /** Folge auf die Vorlage [template] setzen, ihre letzte Phase wählen; Minuten und Name bleiben. */
    fun applyTemplate(template: FreeTemplate) {
        pattern = template.pattern
        selectedValue = pattern.phases.lastIndex
    }

    /** Folge zurück auf den Stand beim Öffnen, letzte Phase gewählt; Minuten und Name bleiben. */
    fun reset() {
        pattern = initial
        selectedValue = pattern.phases.lastIndex
    }

    /** Name aus dem Textfeld, auf [NAME_MAX] Zeichen gekürzt; ab jetzt folgt er nicht mehr den Zahlen. */
    fun updateName(text: String) {
        typedName = text.take(NAME_MAX)
        nameEdited = true
    }

    /**
     * Der Stand, der gespeichert würde (Folge, Minuten, bereinigter Name). Der Aufrufer bildet
     * ihn **vor** dem Schreiben, schreibt genau diese Werte und gibt ihn an [markSaved] zurück.
     */
    fun draft(): PatternSnapshot = current()

    /**
     * Der Stand [written] wurde unter [id] gespeichert (neu angelegt oder aktualisiert). Hat
     * sich der Entwurf während des Schreibens geändert (Kurve weitergezogen), bleibt das
     * Lesezeichen leer – gespeichert ist ja der alte Stand.
     */
    fun markSaved(id: Long, written: PatternSnapshot = current()) {
        savedId = id
        snapshot = written
    }

    /**
     * Der Eintrag wurde aus „Meine Muster“ entfernt. Der Entwurf bleibt sichtbar; ein erneutes
     * Speichern legt einen neuen Eintrag an, den ein weiterer Tipp sofort wieder herausnimmt.
     */
    fun markRemoved() {
        savedId = null
        snapshot = null
        existedBefore = false
    }

    private fun current() = PatternSnapshot(pattern, minutes, savedName)

    companion object {
        /** Höchstlänge des Namens (Nutzerentscheidung 2026-10-22). */
        const val NAME_MAX = PatternBackup.MAX_NAME

        /** Vorgabedauer eines neuen Musters, wie beim Muster des Tages (≈ 5 min). */
        const val DEFAULT_MINUTES = 5

        /** Vorschlagsname zu [pattern], z. B. „Eigenes Muster 4·1·6“. */
        fun suggestedName(namePrefix: String, pattern: FreePattern): String = "$namePrefix ${pattern.numbers()}"

        /** Neues Muster: [pattern] (Vorgabe 4·6, [FreePattern.DEFAULT]), Dauer [minutes]. */
        fun new(
            namePrefix: String,
            pattern: FreePattern = FreePattern.DEFAULT,
            minutes: Int = DEFAULT_MINUTES,
        ): PatternEditorState = PatternEditorState(
            namePrefix = namePrefix,
            pattern = pattern,
            initial = pattern,
            selected = pattern.phases.lastIndex,
            minutes = minutes,
            typedName = "",
            nameEdited = false,
            keptName = "",
            keptFor = pattern,
            savedId = null,
            snapshot = null,
            existedBefore = false,
        )

        /**
         * Ein Muster des Tages als eigenes Muster anpassen: seine vier Felder als freie Folge
         * ([toFreePattern]: Ein, Halten voll, Aus, Halten leer; ganze Sekunden); noch nichts
         * gespeichert.
         */
        fun fromDaily(namePrefix: String, spec: PatternSpec, minutes: Int = DEFAULT_MINUTES): PatternEditorState = new(namePrefix, spec.toFreePattern(), minutes)

        /**
         * Ein gespeichertes Muster bearbeiten ([pattern] = `SavedPattern.pattern`; ältere Muster
         * mit vier Feldern kommen dort schon als Folge an): Lesezeichen gefüllt, Speichern
         * aktualisiert denselben Eintrag [id]. [minutes] null (Muster vor der Herkunfts-Spalte)
         * heißt [DEFAULT_MINUTES].
         *
         * Der gespeicherte [name] folgt weiter den Zahlen, wenn er auf die Zahlen der Folge endet
         * („Eigenes Muster 4·1·6“, „Tagesmuster 4·2·6“): Er bleibt, bis sich die Folge ändert,
         * und heißt dann „Eigenes Muster …“ mit den neuen Zahlen, statt alte Zahlen im Namen zu
         * behalten. Ein frei gewählter Name bleibt stehen.
         */
        fun forEdit(
            namePrefix: String,
            id: Long,
            pattern: FreePattern,
            minutes: Int?,
            name: String,
        ): PatternEditorState {
            val storedMinutes = (minutes ?: DEFAULT_MINUTES).coerceIn(AdjustLimits.MINUTES)
            val followsNumbers = name.trim().endsWith(" ${pattern.numbers()}")
            return PatternEditorState(
                namePrefix = namePrefix,
                pattern = pattern,
                initial = pattern,
                selected = pattern.phases.lastIndex,
                minutes = storedMinutes,
                typedName = if (followsNumbers) "" else name,
                nameEdited = !followsNumbers,
                keptName = if (followsNumbers) name.trim() else "",
                keptFor = pattern,
                savedId = id,
                snapshot = PatternSnapshot(pattern, storedMinutes, PatternBackup.cleanName(name).ifEmpty { suggestedName(namePrefix, pattern) }),
                existedBefore = true,
            )
        }

        /**
         * Sichert den ganzen Zustand über Konfigurationswechsel und Prozesstod. Nur Bundle-Typen
         * ohne null: Folgen in Textform ([FreePattern.toCompact]), keine Id als −1 (Room vergibt
         * Ids ab 1). Eine unlesbare Folge (kann nicht vorkommen) wird zur Vorgabe, eine Auswahl
         * außerhalb der Folge zur letzten Phase.
         */
        fun saver(namePrefix: String): Saver<PatternEditorState, *> = listSaver(
            save = { state ->
                val snap = state.snapshot
                listOf(
                    state.pattern.toCompact(),
                    state.initial.toCompact(),
                    state.selected,
                    state.minutes,
                    state.typedName,
                    state.nameEdited,
                    state.keptName,
                    state.keptFor.toCompact(),
                    state.savedId ?: NO_ID,
                    snap != null,
                    (snap?.pattern ?: state.pattern).toCompact(),
                    snap?.minutes ?: state.minutes,
                    snap?.name ?: "",
                    state.existedBefore,
                )
            },
            restore = { list ->
                val savedId = (list[SAVED_ID] as Long).takeIf { it != NO_ID }
                val snapshot = if (list[HAS_SNAPSHOT] as Boolean) {
                    PatternSnapshot(decode(list[SNAPSHOT_PATTERN]), list[SNAPSHOT_MINUTES] as Int, list[SNAPSHOT_NAME] as String)
                } else {
                    null
                }
                PatternEditorState(
                    namePrefix = namePrefix,
                    pattern = decode(list[PATTERN]),
                    initial = decode(list[INITIAL]),
                    selected = list[SELECTED] as Int,
                    minutes = list[MINUTES] as Int,
                    typedName = list[TYPED_NAME] as String,
                    nameEdited = list[NAME_EDITED] as Boolean,
                    keptName = list[KEPT_NAME] as String,
                    keptFor = decode(list[KEPT_FOR]),
                    savedId = savedId,
                    snapshot = snapshot,
                    existedBefore = list[EXISTED_BEFORE] as Boolean,
                )
            },
        )

        private const val NO_ID = -1L
        private const val PATTERN = 0
        private const val INITIAL = 1
        private const val SELECTED = 2
        private const val MINUTES = 3
        private const val TYPED_NAME = 4
        private const val NAME_EDITED = 5
        private const val KEPT_NAME = 6
        private const val KEPT_FOR = 7
        private const val SAVED_ID = 8
        private const val HAS_SNAPSHOT = 9
        private const val SNAPSHOT_PATTERN = 10
        private const val SNAPSHOT_MINUTES = 11
        private const val SNAPSHOT_NAME = 12
        private const val EXISTED_BEFORE = 13

        private fun decode(value: Any?): FreePattern = FreePattern.parse(value as String) ?: FreePattern.DEFAULT
    }
}

/**
 * [PatternEditorState] über rememberSaveable; neue [keys] (etwa eine andere Muster-Id) legen den
 * Zustand mit [create] neu an. [create] bekommt den Namens-Präfix aus den Ressourcen, z. B.
 * `rememberPatternEditorState(id) { PatternEditorState.forEdit(it, id, pattern, minutes, name) }`.
 */
@Composable
fun rememberPatternEditorState(vararg keys: Any?, create: (namePrefix: String) -> PatternEditorState): PatternEditorState {
    val namePrefix = stringResource(R.string.pattern_editor_title)
    return rememberSaveable(*keys, namePrefix, saver = PatternEditorState.saver(namePrefix)) { create(namePrefix) }
}
