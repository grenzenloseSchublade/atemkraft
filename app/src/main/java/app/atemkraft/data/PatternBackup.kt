package app.atemkraft.data

import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.withinGuardrails
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Datei-Format für Export/Import gespeicherter Muster (JSON). Bewusst nur Werte, keine Seeds,
 * Texte oder Wiederholungszahlen – die baut [app.atemkraft.domain.RandomPatternGenerator.exerciseFrom]
 * beim Laden neu. Eine Import-Datei ist nicht vertrauenswürdige Eingabe: [PatternBackup.decode]
 * begrenzt Größe und Anzahl und verwirft unplausible Einträge, statt abzubrechen.
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
    private const val MAX_NAME = 80

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
        val valid = candidates.filter { it.isPlausible() }
        return DecodedBackup(
            entries = valid,
            invalid = candidates.size - valid.size,
            truncated = file.patterns.size - candidates.size,
        )
    }

    /** Schlüssel für Dubletten: gleiche Herkunft (Zeitpunkt) und gleiche Werte. */
    fun identity(
        createdAtEpochMs: Long,
        inhale: Double,
        holdFull: Double?,
        exhale: Double,
        holdEmpty: Double?,
        activating: Boolean,
    ): String = "$createdAtEpochMs|$inhale|$holdFull|$exhale|$holdEmpty|$activating"

    fun PatternBackupEntry.identity(): String = identity(createdAtEpochMs, inhale, holdFull, exhale, holdEmpty, activating)

    /**
     * Nur was die App selbst erzeugen könnte: Werte in den Leitplanken des Generators (sonst
     * stimmt die Beschreibung nicht und das Muster startet ohne Sicherheitsabfrage), Anpassungen
     * in den Grenzen der Stepper, Name ohne Steuer-, Format- oder Zeilentrennzeichen.
     */
    private fun PatternBackupEntry.isPlausible(): Boolean = isCleanName(name) &&
        createdAtEpochMs > 0 &&
        PatternSpec(inhale, holdFull, exhale, holdEmpty, activating).withinGuardrails() &&
        (overrides?.isPlausible() ?: true)

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
