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
import app.atemkraft.domain.PatternComposer
import app.atemkraft.domain.PatternField
import app.atemkraft.domain.PatternOrigin
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.PatternWish
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.rangeFor
import app.atemkraft.domain.secondsOf
import app.atemkraft.domain.with
import app.atemkraft.domain.withinCreationRules
import kotlin.math.roundToInt

/**
 * Was gespeichert ist: Phasen, Minuten und Name, so wie sie zuletzt geschrieben wurden. Das
 * Lesezeichen ist gefüllt, solange der Entwurf genau diesem Stand entspricht.
 */
data class PatternSnapshot(val spec: PatternSpec, val minutes: Int, val name: String)

/**
 * Zustand des Muster-Editors („Eigenes Muster“, MUSTER-10), getrennt von der Darstellung – nach
 * dem Vorbild von `SessionAdjustState` (ARC-7). Kurve, Stepper, Wunsch-Reihen, Name und
 * Lesezeichen lesen und ändern nur diesen Zustand.
 *
 * **Invariante:** [spec] hält immer die Erzeugungsregeln ein ([withinCreationRules]). Jede
 * Änderung geht über [set] (geklemmt auf [rangeFor]), über einen Vorschlag des
 * [PatternComposer] oder über die Einpassung beim Öffnen ([fitToCreationRules]).
 *
 * - Die Wünsche ([wish]) sind kein eigener Merker, sondern folgen dem Muster: Charakter aus
 *   [PatternSpec.activating], „mit Halten“, sobald eine Haltephase da ist. Wer per Stepper ein
 *   Halten einstellt, sieht also ehrlich „mit“.
 * - [chooseCharacter], [chooseHold] und [nextSuggestion] setzen den Vorschlag und das Muster
 *   gemeinsam; ein Wunschwechsel ersetzt bewusst die Feinwerte (Plan-Risiko R6).
 * - „Angepasst“ ist der Wertvergleich mit dem Vorschlag ([modified]); [reset] setzt die Phasen
 *   zurück, die Minuten bleiben. Die Dauer ändert nie die Phasen.
 * - Der Name folgt dem Vorschlagsnamen der aktuellen Zahlen („Eigenes Muster 4·1·6“), bis die
 *   Person selbst tippt ([updateName], höchstens [NAME_MAX] Zeichen). Leer gespeichert heißt
 *   Vorschlagsname ([savedName]).
 * - [saved]: Es gibt einen gespeicherten Eintrag ([savedId]) und der Entwurf entspricht ihm
 *   ([snapshot]). Jede Änderung an Phasen, Minuten oder Name leert das Lesezeichen.
 *
 * [namePrefix] kommt aus den Ressourcen (`pattern_editor_title`), weil dieser Zustand keine
 * Texte kennen soll.
 */
@Stable
class PatternEditorState internal constructor(
    val namePrefix: String,
    spec: PatternSpec,
    suggestion: PatternSpec,
    variant: Int,
    minutes: Int,
    typedName: String,
    nameEdited: Boolean,
    keptName: String,
    keptFor: PatternSpec,
    savedId: Long?,
    snapshot: PatternSnapshot?,
    existedBefore: Boolean,
    fittedOnOpen: Boolean,
) {
    /** Das eingestellte Muster; hält immer die Erzeugungsregeln ein. */
    var spec: PatternSpec by mutableStateOf(spec)
        private set

    /** Der aktuelle Vorschlag (Ziel von [reset]). */
    var suggestion: PatternSpec by mutableStateOf(suggestion)
        private set

    /**
     * Position von [suggestion] in `PatternComposer.candidates(wish)`; −1, wenn der Vorschlag
     * nicht aus der Liste stammt (übernommenes Tagesmuster, bearbeitetes Muster, Samana).
     */
    var variant: Int by mutableIntStateOf(variant)
        private set

    private var minutesValue: Int by mutableIntStateOf(minutes.coerceIn(AdjustLimits.MINUTES))

    /** Dauer in Minuten (1–30, geklemmt); ändert weder Vorschlag noch Phasen. */
    var minutes: Int
        get() = minutesValue
        set(value) {
            minutesValue = value.coerceIn(AdjustLimits.MINUTES)
        }

    private var typedName: String by mutableStateOf(typedName.take(NAME_MAX))

    /**
     * Ein gespeicherter Name, der den Zahlen folgt („Tagesmuster 4·2·6“), gilt weiter, solange
     * die Phasen [keptFor] entsprechen; danach greift der Vorschlagsname. Leer = keiner.
     */
    private val keptName: String = keptName
    private val keptFor: PatternSpec = keptFor

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

    /**
     * Musste das Ausgangsmuster beim Öffnen eingepasst werden ([fitToCreationRules])? Dann
     * weichen die Zahlen vom gespeicherten Eintrag ab und das Lesezeichen ist anfangs leer.
     */
    val fittedOnOpen: Boolean = fittedOnOpen

    /** Die Wünsche, wie sie das Muster gerade erfüllt (siehe Klassen-KDoc). */
    val wish: PatternWish get() = PatternComposer.wishOf(spec)

    /** Weicht das Muster vom Vorschlag ab (dann zeigt der Start den Reset)? */
    val modified: Boolean get() = spec != suggestion

    /** Vorschlagsname der aktuellen Zahlen, z. B. „Eigenes Muster 4·1·6“. */
    val suggestedName: String get() = suggestedName(namePrefix, spec)

    /**
     * Inhalt des Namensfelds: getippt, sonst der Vorschlagsname – beim Bearbeiten bis zur ersten
     * Änderung der Phasen noch der gespeicherte Name.
     */
    val name: String
        get() = when {
            nameEdited -> typedName
            keptName.isNotEmpty() && spec == keptFor -> keptName
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
        RandomPatternGenerator.exerciseFrom(
            spec = this.spec,
            id = this.savedId?.let { "muster-$it" } ?: RandomPatternGenerator.DRAFT_ID,
            name = savedName,
            origin = PatternOrigin.CUSTOM,
            minutes = this.minutes,
        )
    }

    /**
     * Der Entwurf als lauffähige Übung (Herkunft CUSTOM, Dauer [minutes]): für „Starten“ ohne
     * Speichern und für die Live-Einordnung (`lore`), Musterzeile und Dauer.
     */
    fun exercise(): Exercise = exercise

    /** Die Anpassung für den Start: nur die Dauer, die Phasen stecken schon in [exercise]. */
    fun config(): SessionConfig = SessionConfig(minutes = minutes)

    /** Länge von [field] in ganzen Sekunden (Halten ohne = 0). */
    fun secondsOf(field: PatternField): Int = spec.secondsOf(field)

    /** Erlaubte Werte für [field] (Stepper, Kurven-Griffe); null = nicht wählbar. */
    fun rangeFor(field: PatternField): IntRange? = spec.rangeFor(field)

    /** Setzt [field] auf [seconds], geklemmt auf [rangeFor]; nicht wählbare Felder bleiben. */
    fun set(field: PatternField, seconds: Int) {
        val range = spec.rangeFor(field) ?: return
        spec = spec.with(field, seconds.coerceIn(range))
    }

    /** Charakter wählen: setzt den typischen Vorschlag; derselbe Charakter ändert nichts. */
    fun chooseCharacter(activating: Boolean) {
        if (activating == spec.activating) return
        propose(wish.copy(activating = activating), 0)
    }

    /** Halten „ohne“/„mit“ wählen: setzt den typischen Vorschlag; dieselbe Wahl ändert nichts. */
    fun chooseHold(withHold: Boolean) {
        if (withHold == wish.withHold) return
        propose(wish.copy(withHold = withHold), 0)
    }

    /**
     * „Anderer Vorschlag“: der nächste passende Vorschlag zu den aktuellen Wünschen, ohne Zufall.
     * Erst nach allen passenden Mustern kommt der erste wieder; ein Vorschlag, der nicht aus der
     * Liste stammt, beginnt sie vorn.
     */
    fun nextSuggestion() {
        val current = wish
        val next = if (PatternComposer.wishOf(suggestion) == current) variant + 1 else 0
        propose(current, next)
    }

    /** Phasen zurück auf den Vorschlag; Minuten und Name bleiben. */
    fun reset() {
        spec = suggestion
    }

    /** Name aus dem Textfeld, auf [NAME_MAX] Zeichen gekürzt; ab jetzt folgt er nicht mehr den Zahlen. */
    fun updateName(text: String) {
        typedName = text.take(NAME_MAX)
        nameEdited = true
    }

    /**
     * Der Stand, der gespeichert würde (Phasen, Minuten, bereinigter Name). Der Aufrufer bildet
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

    private fun propose(wish: PatternWish, variant: Int) {
        val size = PatternComposer.candidates(wish).size
        val index = Math.floorMod(variant, size)
        val proposal = PatternComposer.propose(wish, index)
        this.variant = index
        suggestion = proposal
        spec = proposal
    }

    private fun current() = PatternSnapshot(spec, minutes, savedName)

    companion object {
        /** Höchstlänge des Namens (Nutzerentscheidung 2026-10-22). */
        const val NAME_MAX = PatternBackup.MAX_NAME

        /** Vorgabedauer eines neuen Musters, wie beim Muster des Tages (≈ 5 min). */
        const val DEFAULT_MINUTES = 5

        /** Wünsche eines ganz neuen Musters: ruhig, ohne Halten. */
        val DEFAULT_WISH = PatternWish(activating = false, withHold = false)

        /** Vorschlagsname zu [spec], z. B. „Eigenes Muster 4·1·6“. */
        fun suggestedName(namePrefix: String, spec: PatternSpec): String = "$namePrefix ${RandomPatternGenerator.numbersOf(spec)}"

        /** Neues Muster: typischer Vorschlag zu [wish], Dauer [minutes]. */
        fun new(
            namePrefix: String,
            wish: PatternWish = DEFAULT_WISH,
            minutes: Int = DEFAULT_MINUTES,
        ): PatternEditorState {
            val proposal = PatternComposer.propose(wish, 0)
            return PatternEditorState(
                namePrefix = namePrefix,
                spec = proposal,
                suggestion = proposal,
                variant = 0,
                minutes = minutes,
                typedName = "",
                nameEdited = false,
                keptName = "",
                keptFor = proposal,
                savedId = null,
                snapshot = null,
                existedBefore = false,
                fittedOnOpen = false,
            )
        }

        /**
         * Ein Muster des Tages als eigenes Muster anpassen: Ausgangswerte = Vorschlag, Wünsche
         * folgen ihm ([PatternComposer.wishOf]); noch nichts gespeichert.
         */
        fun fromDaily(namePrefix: String, spec: PatternSpec, minutes: Int = DEFAULT_MINUTES): PatternEditorState {
            val fitted = fitToCreationRules(spec)
            return PatternEditorState(
                namePrefix = namePrefix,
                spec = fitted,
                suggestion = fitted,
                variant = indexIn(fitted),
                minutes = minutes,
                typedName = "",
                nameEdited = false,
                keptName = "",
                keptFor = fitted,
                savedId = null,
                snapshot = null,
                existedBefore = false,
                fittedOnOpen = fitted != spec,
            )
        }

        /**
         * Ein gespeichertes Muster bearbeiten: Ausgangswerte = Vorschlag, Lesezeichen gefüllt,
         * Speichern aktualisiert denselben Eintrag [id]. [minutes] null (Muster vor der
         * Herkunfts-Spalte) heißt [DEFAULT_MINUTES].
         *
         * Der gespeicherte [name] folgt weiter den Zahlen, wenn er auf die Zahlen des
         * gespeicherten Musters endet („Eigenes Muster 4·1·6“, „Tagesmuster 4·2·6“): Er bleibt,
         * bis sich die Phasen ändern, und heißt dann „Eigenes Muster …“ mit den neuen Zahlen,
         * statt alte Zahlen im Namen zu behalten. Ein frei gewählter Name bleibt stehen.
         *
         * Hält [spec] die Erzeugungsregeln nicht ein (gespeichert bis 1.6.2, siehe
         * [fitToCreationRules]), startet der Editor mit den eingepassten Werten; der
         * [snapshot] bleibt der gespeicherte Stand, das Lesezeichen ist also leer, bis die
         * eingepassten Werte gespeichert sind.
         */
        fun forEdit(
            namePrefix: String,
            id: Long,
            spec: PatternSpec,
            minutes: Int?,
            name: String,
        ): PatternEditorState {
            val fitted = fitToCreationRules(spec)
            val storedMinutes = (minutes ?: DEFAULT_MINUTES).coerceIn(AdjustLimits.MINUTES)
            val followsNumbers = name.trim().endsWith(" ${RandomPatternGenerator.numbersOf(spec)}")
            return PatternEditorState(
                namePrefix = namePrefix,
                spec = fitted,
                suggestion = fitted,
                variant = indexIn(fitted),
                minutes = storedMinutes,
                typedName = if (followsNumbers) "" else name,
                nameEdited = !followsNumbers,
                keptName = if (followsNumbers) name.trim() else "",
                keptFor = spec,
                savedId = id,
                snapshot = PatternSnapshot(spec, storedMinutes, PatternBackup.cleanName(name).ifEmpty { suggestedName(namePrefix, spec) }),
                existedBefore = true,
                fittedOnOpen = fitted != spec,
            )
        }

        /**
         * Passt ein älteres Muster in die Erzeugungsregeln ein, bevor der Editor es zeigt; ein
         * Muster, das sie schon einhält, kommt unverändert zurück. Betroffen sind nur Muster
         * innerhalb der weiteren Import-Leitplanken, wie sie der Generator bis 1.6.2 erzeugte:
         * halbe Sekunden, „sanft aktivierend“ mit gleich langem Ein- und Ausatmen, ruhiger
         * Atemzug 8,5 s.
         *
         * 1. Auf ganze Sekunden runden wie [secondsOf] (4,5 → 5; Halten 0 → ohne), bei „sanft
         *    aktivierend“ fällt Halten leer weg.
         * 2. Reihum auf die Regeln klemmen ([rangeFor]): erst Ausatmen (so wird aus
         *    aktivierend 4·4 das Brmhana 4·3), dann Einatmen, Halten voll, Halten leer;
         *    höchstens [FIT_PASSES] Runden.
         * 3. Gelingt das nicht (nur bei Werten weit außerhalb der Leitplanken), der typische
         *    Vorschlag zu den Wünschen des Musters ([PatternComposer.propose], Variante 0).
         *
         * Der Charakter bleibt dabei immer erhalten.
         */
        fun fitToCreationRules(spec: PatternSpec): PatternSpec {
            if (spec.withinCreationRules()) return spec
            val wish = PatternComposer.wishOf(spec)
            val values = listOfNotNull(spec.inhale, spec.holdFull, spec.exhale, spec.holdEmpty)
            if (values.any { !it.isFinite() }) return PatternComposer.propose(wish, 0)
            var fitted = PatternField.entries.fold(spec) { acc, field ->
                acc.with(field, if (field == PatternField.HOLD_EMPTY && spec.activating) 0 else spec.wholeSeconds(field))
            }
            repeat(FIT_PASSES) {
                if (fitted.withinCreationRules()) return fitted
                for (field in FIT_ORDER) {
                    val range = fitted.rangeFor(field) ?: continue
                    fitted = fitted.with(field, fitted.secondsOf(field).coerceIn(range))
                }
            }
            return if (fitted.withinCreationRules()) fitted else PatternComposer.propose(wish, 0)
        }

        /** Gerundete Sekunden wie [secondsOf], aber für jede Größe (auch negative). */
        private fun PatternSpec.wholeSeconds(field: PatternField): Int = when (field) {
            PatternField.INHALE -> inhale
            PatternField.HOLD_FULL -> holdFull ?: 0.0
            PatternField.EXHALE -> exhale
            PatternField.HOLD_EMPTY -> holdEmpty ?: 0.0
        }.roundToInt()

        private val FIT_ORDER = listOf(PatternField.EXHALE, PatternField.INHALE, PatternField.HOLD_FULL, PatternField.HOLD_EMPTY)
        private const val FIT_PASSES = 3

        /** Position von [spec] unter den Vorschlägen seiner Wünsche, sonst −1. */
        private fun indexIn(spec: PatternSpec): Int = PatternComposer.candidates(PatternComposer.wishOf(spec)).indexOf(spec)

        /**
         * Sichert den ganzen Zustand über Konfigurationswechsel und Prozesstod. Nur Bundle-Typen
         * ohne null: Halten ohne als −1, keine Id als −1 (Room vergibt Ids ab 1).
         */
        fun saver(namePrefix: String): Saver<PatternEditorState, *> = listSaver(
            save = { state ->
                val snap = state.snapshot
                buildList {
                    addAll(encode(state.spec))
                    addAll(encode(state.suggestion))
                    add(state.variant)
                    add(state.minutes)
                    add(state.typedName)
                    add(state.nameEdited)
                    add(state.keptName)
                    addAll(encode(state.keptFor))
                    add(state.savedId ?: NO_ID)
                    add(snap != null)
                    addAll(encode(snap?.spec ?: state.spec))
                    add(snap?.minutes ?: state.minutes)
                    add(snap?.name ?: "")
                    add(state.existedBefore)
                    add(state.fittedOnOpen)
                }
            },
            restore = { list ->
                val savedId = (list[SAVED_ID] as Long).takeIf { it != NO_ID }
                val snapshot = if (list[HAS_SNAPSHOT] as Boolean) {
                    PatternSnapshot(decode(list, SNAPSHOT_SPEC), list[SNAPSHOT_MINUTES] as Int, list[SNAPSHOT_NAME] as String)
                } else {
                    null
                }
                PatternEditorState(
                    namePrefix = namePrefix,
                    spec = decode(list, SPEC),
                    suggestion = decode(list, SUGGESTION),
                    variant = list[VARIANT] as Int,
                    minutes = list[MINUTES] as Int,
                    typedName = list[TYPED_NAME] as String,
                    nameEdited = list[NAME_EDITED] as Boolean,
                    keptName = list[KEPT_NAME] as String,
                    keptFor = decode(list, KEPT_FOR),
                    savedId = savedId,
                    snapshot = snapshot,
                    existedBefore = list[EXISTED_BEFORE] as Boolean,
                    fittedOnOpen = list[FITTED_ON_OPEN] as Boolean,
                )
            },
        )

        private const val NO_ID = -1L
        private const val NO_HOLD = -1.0
        private const val SPEC_SIZE = 5
        private const val SPEC = 0
        private const val SUGGESTION = SPEC + SPEC_SIZE
        private const val VARIANT = SUGGESTION + SPEC_SIZE
        private const val MINUTES = VARIANT + 1
        private const val TYPED_NAME = MINUTES + 1
        private const val NAME_EDITED = TYPED_NAME + 1
        private const val KEPT_NAME = NAME_EDITED + 1
        private const val KEPT_FOR = KEPT_NAME + 1
        private const val SAVED_ID = KEPT_FOR + SPEC_SIZE
        private const val HAS_SNAPSHOT = SAVED_ID + 1
        private const val SNAPSHOT_SPEC = HAS_SNAPSHOT + 1
        private const val SNAPSHOT_MINUTES = SNAPSHOT_SPEC + SPEC_SIZE
        private const val SNAPSHOT_NAME = SNAPSHOT_MINUTES + 1
        private const val EXISTED_BEFORE = SNAPSHOT_NAME + 1
        private const val FITTED_ON_OPEN = EXISTED_BEFORE + 1

        private fun encode(spec: PatternSpec): List<Any> = listOf(spec.inhale, spec.holdFull ?: NO_HOLD, spec.exhale, spec.holdEmpty ?: NO_HOLD, spec.activating)

        private fun decode(list: List<Any?>, at: Int): PatternSpec = PatternSpec(
            inhale = list[at] as Double,
            holdFull = (list[at + 1] as Double).takeIf { it != NO_HOLD },
            exhale = list[at + 2] as Double,
            holdEmpty = (list[at + 3] as Double).takeIf { it != NO_HOLD },
            activating = list[at + 4] as Boolean,
        )
    }
}

/**
 * [PatternEditorState] über rememberSaveable; neue [keys] (etwa eine andere Muster-Id) legen den
 * Zustand mit [create] neu an. [create] bekommt den Namens-Präfix aus den Ressourcen, z. B.
 * `rememberPatternEditorState(id) { PatternEditorState.forEdit(it, id, spec, minutes, name) }`.
 */
@Composable
fun rememberPatternEditorState(vararg keys: Any?, create: (namePrefix: String) -> PatternEditorState): PatternEditorState {
    val namePrefix = stringResource(R.string.pattern_editor_title)
    return rememberSaveable(*keys, namePrefix, saver = PatternEditorState.saver(namePrefix)) { create(namePrefix) }
}
