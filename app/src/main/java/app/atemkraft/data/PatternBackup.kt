package app.atemkraft.data

import app.atemkraft.data.local.SavedPatternEntity
import app.atemkraft.data.local.columnSpec
import app.atemkraft.data.local.freePattern
import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.Feeling
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.GONG_INTERVAL_MAX_MINUTES
import app.atemkraft.domain.GONG_INTERVAL_MIN_MINUTES
import app.atemkraft.domain.MEDITATION_MAX_MINUTES
import app.atemkraft.domain.MEDITATION_MIN_MINUTES
import app.atemkraft.domain.MeditationMode
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.SessionKind
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
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
 *
 * **Version 2 (ab 2.9.0) ist die Sicherung „alles“:** neben den Mustern das Logbuch samt Befinden
 * ([logbook]), die Markierungen „Tut mir gut“ ([marked]) und die Einstellungen ohne
 * Gerätespezifisches ([settings]). Der Formatname bleibt „atemkraft-muster“, damit es eine
 * Dateifamilie bleibt; Dateien der Version 1 (nur Muster) liest diese Version weiter, ältere
 * App-Versionen weisen Version 2 ab, statt die Hälfte still zu verlieren.
 */
@Serializable
data class PatternBackupFile(
    val format: String = PatternBackup.FORMAT,
    val version: Int = PatternBackup.VERSION,
    val exportedAtEpochMs: Long,
    val patterns: List<PatternBackupEntry>,
    val logbook: List<LogBackupEntry> = emptyList(),
    val marked: List<String> = emptyList(),
    val settings: SettingsBackup? = null,
)

/**
 * Ein Logbuch-Eintrag in der Sicherung; Enums unter ihrem Namen wie in der Datenbank. Beim Einlesen
 * nicht vertrauenswürdig ([PatternBackup.decode]): unbekannte Art oder Familie verwirft den
 * Eintrag, ein unbekanntes Befinden gilt als nicht beantwortet.
 */
@Serializable
data class LogBackupEntry(
    val exerciseId: String,
    val exerciseName: String,
    val family: String? = null,
    val kind: String = "BREATHING",
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val roundsCompleted: Int = 0,
    val feeling: String? = null,
)

/**
 * Einstellungen in der Sicherung: nur, was auf jedem Gerät gleich gilt. Nicht darin: Stimme (muss
 * neu geladen werden), Erinnerung (braucht die Benachrichtigungs-Erlaubnis des neuen Geräts),
 * bestätigte Sicherheitshinweise (lieber einmal zu oft), Willkommenskarte, gefragte Erlaubnis,
 * Marken einer beschädigten Datei. Jedes Feld ist optional; ein fehlendes oder unplausibles lässt
 * die Einstellung auf dem Gerät, wie sie ist. [adjustments]: Anpassungen eingebauter Übungen (die
 * der Muster stehen am Muster).
 */
@Serializable
data class SettingsBackup(
    val soundMode: String? = null,
    val transition: String? = null,
    val volume: String? = null,
    val haptics: Boolean? = null,
    val gongLong: Boolean? = null,
    val showSafetyWarning: Boolean? = null,
    val showNextPhase: Boolean? = null,
    val showProgress: Boolean? = null,
    val showSeconds: Boolean? = null,
    val autoCalm: Boolean? = null,
    val silenceAfter: Boolean? = null,
    val meditationAutoCalm: Boolean? = null,
    val meditationShowProgress: Boolean? = null,
    val meditationShowTime: Boolean? = null,
    val meditationMode: String? = null,
    val meditationMinutes: Int? = null,
    val meditationStartEndGong: Boolean? = null,
    val meditationInterval: Boolean? = null,
    val meditationSpeech: Boolean? = null,
    val gongIntervalMinutes: Int? = null,
    val showStreak: Boolean? = null,
    val askFeeling: Boolean? = null,
    val adjustments: Map<String, PatternBackupOverrides> = emptyMap(),
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
 * Ergebnis des Einlesens: gültige Muster ([entries]), Logbuch-Einträge ([logbook]), Markierungen
 * ([marked]) und Einstellungen ([settings], schon auf Plausibles reduziert); [invalid] verworfene
 * Muster und Logbuch-Einträge, [truncated] über [PatternBackup.MAX_PATTERNS] und [logTruncated] über
 * [PatternBackup.MAX_LOG_ENTRIES] nicht gelesene.
 */
data class DecodedBackup(
    val entries: List<PatternBackupEntry>,
    val invalid: Int,
    val truncated: Int = 0,
    val logbook: List<SessionLogEntry> = emptyList(),
    val logTruncated: Int = 0,
    val marked: Set<String> = emptySet(),
    val settings: SettingsBackup? = null,
) {
    /** Nichts darin, das sich übernehmen ließe. */
    val isEmpty: Boolean get() = entries.isEmpty() && logbook.isEmpty() && marked.isEmpty() && settings == null
}

object PatternBackup {
    const val FORMAT = "atemkraft-muster"
    const val VERSION = 2

    /**
     * Obergrenzen gegen manipulierte oder versehentlich gewählte große Dateien. 8 MiB tragen
     * [MAX_LOG_ENTRIES] Einträge (je rund 250 Bytes) – Jahre täglichen Übens.
     */
    const val MAX_BYTES = 8 * 1024 * 1024
    const val MAX_PATTERNS = 500
    const val MAX_LOG_ENTRIES = 20_000
    const val MAX_MARKED = 200

    /** Höchstdauer eines Logbuch-Eintrags (eine offene Meditation von einem Tag). */
    private const val MAX_DURATION_MS = 24 * 60 * 60 * 1000L

    /** Höchstzahl Runden eines Eintrags (weit über jeder Übung). */
    private const val MAX_ROUNDS = 10_000

    /** Erlaubte Zeichen einer Übungs-Id (eingebaute, „muster-12“, „meditation“). */
    private val EXERCISE_ID = Regex("[A-Za-z0-9_-]{1,64}")

    /** Höchstlänge eines Musternamens (Import und Editor). */
    const val MAX_NAME = 80

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(file: PatternBackupFile): String = json.encodeToString(PatternBackupFile.serializer(), file)

    /**
     * Liest eine Sicherung (Version 1: nur Muster, Version 2: alles); null, wenn sie keine
     * Atemkraft-Sicherung ist. Die Byte-Grenze setzt schon der Aufrufer beim Lesen ([MAX_BYTES]);
     * hier nur noch als Zeichen-Obergrenze. [builtInIds] sind die Ids eingebauter Übungen: Nur sie
     * dürfen markiert sein oder eine Anpassung in den Einstellungen tragen.
     */
    fun decode(text: String, builtInIds: Set<String> = emptySet()): DecodedBackup? {
        if (text.length > MAX_BYTES) return null
        val file = runCatching { json.decodeFromString(PatternBackupFile.serializer(), text) }.getOrNull()
            ?: return null
        if (file.format != FORMAT || file.version !in 1..VERSION) return null
        val candidates = file.patterns.take(MAX_PATTERNS)
        val valid = candidates.filter { it.isPlausible() }.map { it.normalized() }
        val logCandidates = file.logbook.take(MAX_LOG_ENTRIES)
        val log = logCandidates.mapNotNull { it.toDomainOrNull() }
        return DecodedBackup(
            entries = valid,
            invalid = candidates.size - valid.size + logCandidates.size - log.size,
            truncated = file.patterns.size - candidates.size,
            logbook = log,
            logTruncated = file.logbook.size - logCandidates.size,
            marked = file.marked.filter { it in builtInIds }.take(MAX_MARKED).toSet(),
            settings = file.settings?.plausible(builtInIds),
        )
    }

    /** Der Eintrag eines Logbuch-Eintrags in der Sicherung. */
    fun logEntry(entry: SessionLogEntry): LogBackupEntry = LogBackupEntry(
        exerciseId = entry.exerciseId,
        exerciseName = entry.exerciseName,
        family = entry.family?.name,
        kind = entry.kind.name,
        startedAtEpochMs = entry.startedAtEpochMs,
        durationMs = entry.durationMs,
        roundsCompleted = entry.roundsCompleted,
        feeling = entry.feeling?.name,
    )

    /**
     * Ein Eintrag, den die App selbst geschrieben haben könnte, sonst null: bekannte Art, bei
     * Atemübungen eine bekannte Familie (Meditation ohne), Id und Name sauber, Zeiten in den Grenzen.
     */
    private fun LogBackupEntry.toDomainOrNull(): SessionLogEntry? {
        val kind = SessionKind.entries.firstOrNull { it.name == kind } ?: return null
        val family = family?.let { name -> BreathingFamily.entries.firstOrNull { it.name == name } ?: return null }
        val plausible = EXERCISE_ID.matches(exerciseId) && isCleanName(exerciseName) && startedAtEpochMs > 0 &&
            durationMs in 0..MAX_DURATION_MS && roundsCompleted in 0..MAX_ROUNDS &&
            (kind == SessionKind.MEDITATION) == (family == null)
        if (!plausible) return null
        return SessionLogEntry(
            exerciseId = exerciseId,
            exerciseName = exerciseName.trim(),
            family = family,
            startedAtEpochMs = startedAtEpochMs,
            durationMs = durationMs,
            roundsCompleted = roundsCompleted,
            kind = kind,
            feeling = Feeling.fromName(feeling),
        )
    }

    /**
     * Nur plausible Felder: Enum-Namen, die es gibt, Minuten in den Grenzen der App, Anpassungen nur
     * eingebauter Übungen in den Stepper-Grenzen. Alles andere wird `null` (bleibt auf dem Gerät).
     */
    private fun SettingsBackup.plausible(builtInIds: Set<String>): SettingsBackup = copy(
        soundMode = soundMode?.takeIf { n -> SoundMode.entries.any { it.name == n } },
        transition = transition?.takeIf { n -> TransitionEmphasis.entries.any { it.name == n } },
        volume = volume?.takeIf { n -> ToneVolume.entries.any { it.name == n } },
        meditationMode = meditationMode?.takeIf { n -> MeditationMode.entries.any { it.name == n } },
        meditationMinutes = meditationMinutes?.takeIf { it in MEDITATION_MIN_MINUTES..MEDITATION_MAX_MINUTES },
        gongIntervalMinutes = gongIntervalMinutes?.takeIf { it in GONG_INTERVAL_MIN_MINUTES..GONG_INTERVAL_MAX_MINUTES },
        adjustments = adjustments.entries.filter { (id, o) -> id in builtInIds && o.isPlausible() }.take(MAX_MARKED).associate { it.toPair() },
    )

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
