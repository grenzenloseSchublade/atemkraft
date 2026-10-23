package app.atemkraft.data

import app.atemkraft.data.local.SavedPatternEntity
import app.atemkraft.data.local.columnSpec
import app.atemkraft.data.local.freePattern
import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.withinGuardrails
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Datei-Format für Export/Import gespeicherter Muster (JSON). Bewusst nur Werte, Herkunft und
 * Dauer, keine Seeds, Texte oder Wiederholungszahlen – die baut
 * [app.atemkraft.domain.GeneratedExercises.exerciseFrom] beim Laden aus Werten, Herkunft
 * (`origin`) und Dauer (`minutes`) neu. Eine Import-Datei ist nicht vertrauenswürdige Eingabe:
 * [PatternBackup.decode] begrenzt Größe und Anzahl und verwirft unplausible Einträge, statt
 * abzubrechen.
 *
 * `origin` und `minutes` kamen mit dem Muster-Editor hinzu, ohne die [PatternBackup.VERSION] zu
 * erhöhen: Beide sind optional, Dateien von 1.6.8 und älter gelten als gespeicherte Tagesmuster
 * ohne eigene Dauer. Ältere App-Versionen überlesen die Felder (`ignoreUnknownKeys`); ein eigenes
 * Muster zeigt dort die Texte eines gespeicherten Tagesmusters und dauert etwa 5 min.
 *
 * Ebenso optional kam `phases` hinzu, die freie Phasenfolge eines eigenen Musters („Frei
 * gestalten“, Textform von [app.atemkraft.domain.FreePattern.toCompact]). Die vier Felder
 * tragen dann deren Zusammenfassung: Ältere App-Versionen lesen nur sie (und verwerfen den
 * Eintrag, wenn die Zusammenfassung ihre Leitplanken verlässt); diese Version nimmt die Folge und
 * rechnet die vier Felder daraus neu.
 */
@Serializable
data class PatternBackupFile(
    val format: String = PatternBackup.FORMAT,
    val version: Int = PatternBackup.VERSION,
    val exportedAtEpochMs: Long,
    val patterns: List<PatternBackupEntry>,
)

@Serializable
data class PatternBackupEntry(
    val name: String,
    val inhale: Double,
    val holdFull: Double? = null,
    val exhale: Double,
    val holdEmpty: Double? = null,
    val activating: Boolean,
    val createdAtEpochMs: Long,
    /** Session-Anpassung des Musters (Dauer in Minuten, Phasen in Sekunden); null = nie angepasst. */
    val overrides: PatternBackupOverrides? = null,
    /**
     * Herkunft: „daily“ (gespeichertes Muster des Tages) oder „custom“ (eigenes Muster). Fehlt
     * sie, ist es ein Tagesmuster (Dateien bis 1.6.8); ein unbekannter Wert gilt als eigenes Muster.
     */
    val origin: String? = null,
    /** Vorgabedauer eigener Muster in Minuten (1–30); null = ≈ 5 min wie beim Muster des Tages. */
    val minutes: Int? = null,
    /**
     * Freie Phasenfolge eines eigenen Musters, z. B. „in:4,hold:1,out:6“; null = es gelten die
     * vier Felder (gespeicherte Tagesmuster, Dateien bis 1.6.8). Nur zusammen mit einer Herkunft
     * außer „daily“ plausibel.
     */
    val phases: String? = null,
)

@Serializable
data class PatternBackupOverrides(
    val duration: Int? = null,
    val inhale: Int? = null,
    val hold: Int? = null,
    val exhale: Int? = null,
)

/**
 * Ergebnis des Einlesens: gültige Einträge, verworfene (außerhalb der Grenzen) und wegen
 * [PatternBackup.MAX_PATTERNS] nicht gelesene.
 */
data class DecodedBackup(val entries: List<PatternBackupEntry>, val invalid: Int, val truncated: Int = 0)

object PatternBackup {
    const val FORMAT = "atemkraft-muster"
    const val VERSION = 1

    /** Obergrenzen gegen manipulierte oder versehentlich gewählte große Dateien. */
    const val MAX_BYTES = 512 * 1024
    const val MAX_PATTERNS = 500

    /** Höchstlänge eines Musternamens (Import und Editor). */
    const val MAX_NAME = 80

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(file: PatternBackupFile): String = json.encodeToString(PatternBackupFile.serializer(), file)

    /**
     * Liest eine Export-Datei; null, wenn sie kein Atemkraft-Muster-Export ist. Die Byte-Grenze
     * setzt schon der Aufrufer beim Lesen ([MAX_BYTES]); hier nur noch als Zeichen-Obergrenze.
     */
    fun decode(text: String): DecodedBackup? {
        if (text.length > MAX_BYTES) return null
        val file = runCatching { json.decodeFromString(PatternBackupFile.serializer(), text) }.getOrNull()
            ?: return null
        if (file.format != FORMAT || file.version !in 1..VERSION) return null
        val candidates = file.patterns.take(MAX_PATTERNS)
        val valid = candidates.filter { it.isPlausible() }.map { it.normalized() }
        return DecodedBackup(
            entries = valid,
            invalid = candidates.size - valid.size,
            truncated = file.patterns.size - candidates.size,
        )
    }

    /**
     * Schlüssel für Dubletten: gleiche Herkunft (Zeitpunkt) und gleiche Werte. Mit freier Folge
     * ([phases], Normalform von [FreePattern.toCompact]) zählen nur Zeitpunkt und Folge – die vier
     * Felder sind dann bloß deren Zusammenfassung.
     */
    fun identity(
        createdAtEpochMs: Long,
        inhale: Double,
        holdFull: Double?,
        exhale: Double,
        holdEmpty: Double?,
        activating: Boolean,
        phases: String? = null,
    ): String = if (phases != null) {
        "$createdAtEpochMs|$phases"
    } else {
        "$createdAtEpochMs|$inhale|$holdFull|$exhale|$holdEmpty|$activating"
    }

    /** Schlüssel einer gespeicherten Zeile; eine unlesbare Folge zählt nicht (dann die vier Felder). */
    fun identity(row: SavedPatternEntity): String = with(row) {
        identity(createdAtEpochMs, inhale, holdFull, exhale, holdEmpty, activating, freePattern()?.toCompact())
    }

    /** Schlüssel eines Datei-Eintrags mit seiner gelesenen Folge [free] (`null` = vier Felder). */
    fun identity(entry: PatternBackupEntry, free: FreePattern? = FreePattern.parse(entry.phases)): String = with(entry) {
        identity(createdAtEpochMs, inhale, holdFull, exhale, holdEmpty, activating, free?.toCompact())
    }

    /**
     * Macht aus einer Namenseingabe einen Namen, den auch der Import annimmt: Steuer-, Format- und
     * Zeilentrennzeichen werden zu Leerzeichen, Leerraum wird zusammengefasst, höchstens
     * [MAX_NAME] Zeichen. Ein leeres Ergebnis heißt „kein Name“ (der Aufrufer setzt einen Vorschlag).
     */
    fun cleanName(raw: String): String = raw
        .map { if (it.isHiddenOrBreaking()) ' ' else it }
        .joinToString("")
        .split(WHITESPACE)
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .take(MAX_NAME)
        .trimEnd()

    private val WHITESPACE = Regex("\\s+")

    /**
     * Nur was die App selbst erzeugen könnte: Werte in den Leitplanken des Generators (sonst
     * stimmt die Beschreibung nicht und das Muster startet ohne Sicherheitsabfrage), Anpassungen
     * in den Grenzen der Stepper, Dauer 1–30 min, Name ohne Steuer-, Format- oder
     * Zeilentrennzeichen. Eine unbekannte Herkunft verwirft nichts, sie gilt als eigenes Muster.
     *
     * Mit freier Folge gelten statt der Leitplanken die Regeln der Folge ([FreePattern.parse]:
     * 2–12 Phasen, je 1–30 s, mindestens ein Ein und ein Aus); die vier Felder prüft die App dann
     * nicht, sie rechnet sie neu ([normalized]). Eine Folge ohne Herkunft oder bei einem
     * Tagesmuster schreibt die App nie – der Eintrag wird verworfen.
     */
    private fun PatternBackupEntry.isPlausible(): Boolean = isCleanName(name) &&
        createdAtEpochMs > 0 &&
        valuesPlausible() &&
        (minutes == null || minutes in AdjustLimits.MINUTES) &&
        (overrides?.isPlausible() ?: true)

    private fun PatternBackupEntry.valuesPlausible(): Boolean = if (phases == null) {
        PatternSpec(inhale, holdFull, exhale, holdEmpty, activating).withinGuardrails()
    } else {
        origin != null && origin != SavedPatternEntity.ORIGIN_DAILY && FreePattern.parse(phases) != null
    }

    /**
     * Ein plausibler Eintrag mit Folge in Normalform: Folge wie [FreePattern.toCompact], die vier
     * Felder als ihre Zusammenfassung ([columnSpec]). Einträge ohne Folge bleiben, wie sie sind.
     */
    private fun PatternBackupEntry.normalized(): PatternBackupEntry {
        val free = FreePattern.parse(phases) ?: return this
        val spec = free.columnSpec()
        return copy(
            inhale = spec.inhale,
            holdFull = spec.holdFull,
            exhale = spec.exhale,
            holdEmpty = spec.holdEmpty,
            activating = spec.activating,
            phases = free.toCompact(),
        )
    }

    private fun PatternBackupOverrides.isPlausible(): Boolean = (duration == null || duration in AdjustLimits.MINUTES) &&
        (inhale == null || inhale in AdjustLimits.INHALE_S) &&
        (hold == null || hold in AdjustLimits.HOLD_S) &&
        (exhale == null || exhale in AdjustLimits.EXHALE_S)

    private fun isCleanName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.isNotEmpty() && trimmed.length <= MAX_NAME && trimmed.none { it.isHiddenOrBreaking() }
    }

    private fun Char.isHiddenOrBreaking(): Boolean = when (Character.getType(this).toByte()) {
        Character.CONTROL, Character.FORMAT, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR,
        Character.PRIVATE_USE, Character.SURROGATE, Character.UNASSIGNED,
        -> true

        else -> false
    }
}
