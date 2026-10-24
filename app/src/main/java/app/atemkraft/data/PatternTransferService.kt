package app.atemkraft.data

import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Datenlogik der Sicherung „alles“ ([PatternBackup], Version 2): eigene Kreationen samt
 * Anpassungen, Logbuch samt Befinden, Markierungen „Tut mir gut“ und Einstellungen ohne
 * Gerätespezifisches in einer Datei. Eingelesen wird in zwei Schritten: [read] prüft die Datei und
 * liefert eine Vorschau, erst [apply] schreibt – zusammenführen statt ersetzen (Kreationen und
 * Logbuch ohne Dubletten, Markierungen dazu), Einstellungen nur auf Wunsch. Woher die Datei kommt
 * (Storage Access Framework), auf welchem Thread es läuft und welche Texte das Ergebnis meldet,
 * entscheidet der Aufrufer (`rememberPatternTransfer`).
 *
 * [builtInIds]: Ids der eingebauten Übungen – nur sie tragen Markierungen und Anpassungen in den
 * Einstellungen (die der Muster stehen am Muster).
 */
class PatternTransferService(
    private val saved: SavedPatternsRepository,
    private val settings: SettingsRepository,
    private val logbook: LogbookRepository,
    private val builtInIds: Set<String> = BuiltInExercises.all.map { it.id }.toSet(),
) {
    /**
     * Schreibt die Sicherung in den Strom, den [open] liefert – erst nachdem alle Daten gelesen sind.
     * Gibt die Zahl der Kreationen und Logbuch-Einträge zurück.
     */
    suspend fun export(nowEpochMs: Long, open: () -> OutputStream): ExportSummary {
        val patterns = saved.exportFile(nowEpochMs) { id -> settings.exerciseIntervals(id).first() }
        val file = patterns.copy(
            logbook = logbook.snapshot().map(PatternBackup::logEntry),
            marked = settings.markedExercises.first().filter { it in builtInIds }.sorted(),
            settings = settings.exportSettings(builtInIds),
        )
        open().bufferedWriter().use { it.write(PatternBackup.encode(file)) }
        return ExportSummary(patterns = file.patterns.size, logbook = file.logbook.size)
    }

    /**
     * Liest höchstens [PatternBackup.MAX_BYTES] aus dem Strom, den [open] liefert, und prüft die Datei
     * als nicht vertrauenswürdige Eingabe; geschrieben wird nichts. Eine größere Datei wird nicht ganz
     * geladen.
     */
    suspend fun read(open: () -> InputStream): ReadOutcome {
        val bytes = readAtMost(open(), PatternBackup.MAX_BYTES) ?: return ReadOutcome.TooLarge
        val decoded = PatternBackup.decode(String(bytes, Charsets.UTF_8), builtInIds) ?: return ReadOutcome.NotBackup
        return ReadOutcome.Preview(decoded)
    }

    /**
     * Übernimmt eine gelesene Sicherung: Kreationen (Dubletten bleiben einmal) samt Anpassungen,
     * Logbuch (Art, Übung und Startzeit gleich = vorhanden), Markierungen dazu; die Einstellungen nur
     * mit [withSettings].
     */
    suspend fun apply(decoded: DecodedBackup, withSettings: Boolean): ApplyResult {
        val patterns = saved.import(decoded.entries) { id, o ->
            settings.setExerciseIntervals(id, o.duration, o.inhale, o.hold, o.exhale)
        }
        val logAdded = logbook.merge(decoded.logbook)
        settings.addMarkedExercises(decoded.marked)
        val settingsApplied = withSettings && decoded.settings != null
        if (settingsApplied) decoded.settings?.let { settings.importSettings(it) }
        return ApplyResult(
            added = patterns.added + logAdded,
            duplicates = patterns.duplicates + decoded.logbook.size - logAdded,
            settingsApplied = settingsApplied,
        )
    }

    /** Was die Sicherung enthält: Zahl der Kreationen und Logbuch-Einträge. */
    data class ExportSummary(val patterns: Int, val logbook: Int)

    /** Ergebnis des Lesens. */
    sealed interface ReadOutcome {
        /** Die Datei ist größer als [PatternBackup.MAX_BYTES]. */
        data object TooLarge : ReadOutcome

        /** Keine Sicherungsdatei der App. */
        data object NotBackup : ReadOutcome

        /** Gelesen und geprüft; [decoded] zeigt, was übernommen würde. */
        data class Preview(val decoded: DecodedBackup) : ReadOutcome
    }

    /**
     * Ergebnis des Übernehmens: [added] neue Kreationen und Logbuch-Einträge, [duplicates] schon
     * vorhandene; [settingsApplied], ob die Einstellungen ersetzt wurden.
     */
    data class ApplyResult(val added: Int, val duplicates: Int, val settingsApplied: Boolean)

    private companion object {
        /** Liest höchstens [limit] Bytes; null, wenn der Strom länger ist (wird nicht ganz geladen). */
        fun readAtMost(stream: InputStream, limit: Int): ByteArray? = stream.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER)
            var total = 0
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                total += n
                if (total > limit) return null
                out.write(buffer, 0, n)
            }
            out.toByteArray()
        }

        const val BUFFER = 64 * 1024
    }
}
