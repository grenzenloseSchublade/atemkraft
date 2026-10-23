package app.atemkraft.data

import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream

/**
 * Datenlogik von Export und Import gespeicherter Muster ([PatternBackup]): liest die Muster samt
 * ihrer Anpassungen, kodiert sie, dekodiert eine Import-Datei und übernimmt deren Einträge.
 * Woher die Datei kommt (Storage Access Framework), auf welchem Thread es läuft und welche Texte
 * das Ergebnis meldet, entscheidet der Aufrufer (`rememberPatternTransfer`).
 */
class PatternTransferService(
    private val saved: SavedPatternsRepository,
    private val settings: SettingsRepository,
) {
    /** Gibt es nichts zu exportieren? */
    suspend fun isEmpty(): Boolean = saved.isEmpty()

    /**
     * Schreibt alle Muster mit ihren Anpassungen als Sicherungsdatei in den Strom, den [open]
     * liefert – erst nachdem die Daten gelesen sind. Gibt die Zahl der Muster zurück.
     */
    suspend fun export(nowEpochMs: Long, open: () -> OutputStream): Int {
        val file = saved.exportFile(nowEpochMs) { id -> settings.exerciseIntervals(id).first() }
        open().bufferedWriter().use { it.write(PatternBackup.encode(file)) }
        return file.patterns.size
    }

    /**
     * Liest höchstens [PatternBackup.MAX_BYTES] aus dem Strom, den [open] liefert, und übernimmt
     * die gültigen Einträge samt Anpassungen. Eine größere Datei wird nicht ganz geladen.
     */
    suspend fun import(open: () -> InputStream): ImportOutcome {
        val bytes = readAtMost(open(), PatternBackup.MAX_BYTES) ?: return ImportOutcome.TooLarge
        val decoded = PatternBackup.decode(String(bytes, Charsets.UTF_8)) ?: return ImportOutcome.NotBackup
        val result = saved.import(decoded.entries) { id, o ->
            settings.setExerciseIntervals(id, o.duration, o.inhale, o.hold, o.exhale)
        }
        return ImportOutcome.Done(
            entries = decoded.entries.size,
            added = result.added,
            duplicates = result.duplicates,
            invalid = decoded.invalid,
            truncated = decoded.truncated,
        )
    }

    /** Ergebnis eines Imports. */
    sealed interface ImportOutcome {
        /** Die Datei ist größer als [PatternBackup.MAX_BYTES]. */
        data object TooLarge : ImportOutcome

        /** Keine Sicherungsdatei der App. */
        data object NotBackup : ImportOutcome

        /**
         * Gelesen: [entries] gültige Einträge, davon [added] neu und [duplicates] schon vorhanden;
         * [invalid] verworfene Einträge, [truncated] über der Höchstzahl abgeschnittene.
         */
        data class Done(val entries: Int, val added: Int, val duplicates: Int, val invalid: Int, val truncated: Int) : ImportOutcome
    }

    private companion object {
        /** Liest höchstens [limit] Bytes; null, wenn der Strom länger ist (wird nicht ganz geladen). */
        fun readAtMost(stream: InputStream, limit: Int): ByteArray? = stream.use { input ->
            val buffer = ByteArray(limit + 1)
            var read = 0
            while (read < buffer.size) {
                val n = input.read(buffer, read, buffer.size - read)
                if (n < 0) break
                read += n
            }
            if (read > limit) null else buffer.copyOf(read)
        }
    }
}
