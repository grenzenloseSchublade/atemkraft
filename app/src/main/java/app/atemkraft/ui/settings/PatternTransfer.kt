package app.atemkraft.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.atemkraft.R
import app.atemkraft.data.PatternBackup
import app.atemkraft.data.SavedPatternsRepository
import app.atemkraft.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate

/**
 * Export/Import gespeicherter Muster für die Einstellungen. Erfolg erscheint als neues Label
 * des auslösenden Buttons (MUSTER-02), Hinweise und Fehler als Zeile in der Karte (MUSTER-04).
 */
class PatternTransfer(
    val exportLabel: String?,
    val importLabel: String?,
    val note: String?,
    val export: () -> Unit,
    val import: () -> Unit,
)

/** Ergebnis eines Vorgangs: neues Button-Label (nur bei Erfolg) und/oder Hinweiszeile. */
private data class Outcome(val label: String? = null, val note: String? = null)

/**
 * Verdrahtet Export/Import über das Storage Access Framework: Der Nutzer wählt Datei und Ort
 * selbst, die App braucht keine Speicher-Berechtigung und behält keinen Zugriff (SEC-STORE-01).
 * Lesen/Schreiben läuft auf [Dispatchers.IO] und [NonCancellable] – ein Zurück während des
 * Imports hinterlässt so kein Muster ohne seine Anpassung.
 */
@Composable
fun rememberPatternTransfer(saved: SavedPatternsRepository, settings: SettingsRepository): PatternTransfer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var importLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf<String?>(null) }

    fun show(isExport: Boolean, outcome: Outcome) {
        if (isExport) exportLabel = outcome.label else importLabel = outcome.label
        note = outcome.note
    }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(MIME_JSON),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch { show(isExport = true, exportTo(context, uri, saved, settings)) }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch { show(isExport = false, importFrom(context, uri, saved, settings)) }
    }

    return PatternTransfer(
        exportLabel = exportLabel,
        importLabel = importLabel,
        note = note,
        export = {
            scope.launch {
                if (saved.isEmpty()) {
                    show(isExport = true, Outcome(note = context.getString(R.string.data_export_empty)))
                } else {
                    createLauncher.launch("atemkraft-muster-${LocalDate.now()}.json")
                }
            }
        },
        // Manche Dateimanager melden JSON als octet-stream oder text/plain.
        import = { openLauncher.launch(arrayOf(MIME_JSON, "text/plain", "application/octet-stream")) },
    )
}

private const val MIME_JSON = "application/json"

/** Gemeinsamer Rahmen: IO-Thread, nicht abbrechbar, jeder Fehler als Hinweis statt Absturz. */
private suspend fun guarded(context: Context, block: suspend () -> Outcome): Outcome = withContext(Dispatchers.IO + NonCancellable) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        Outcome(note = context.getString(R.string.data_io_error))
    } catch (_: RuntimeException) {
        // Fremde DocumentsProvider werfen auch IllegalArgument-/UnsupportedOperation-
        // Exceptions, Room bei vollem Speicher SQLiteFullException. Bewusst ohne Log und
        // ohne e.message (SEC-PRIV-03, MUSTER-04).
        Outcome(note = context.getString(R.string.data_io_error))
    }
}

private suspend fun exportTo(
    context: Context,
    uri: Uri,
    saved: SavedPatternsRepository,
    settings: SettingsRepository,
): Outcome = guarded(context) {
    val file = saved.exportFile(System.currentTimeMillis()) { id -> settings.exerciseIntervals(id).first() }
    val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException()
    stream.bufferedWriter().use { it.write(PatternBackup.encode(file)) }
    Outcome(label = context.resources.getQuantityString(R.plurals.data_export_done, file.patterns.size, file.patterns.size))
}

private suspend fun importFrom(
    context: Context,
    uri: Uri,
    saved: SavedPatternsRepository,
    settings: SettingsRepository,
): Outcome = guarded(context) {
    val bytes = readAtMost(context, uri, PatternBackup.MAX_BYTES)
        ?: return@guarded Outcome(note = context.getString(R.string.data_import_too_large))
    val decoded = PatternBackup.decode(String(bytes, Charsets.UTF_8))
        ?: return@guarded Outcome(note = context.getString(R.string.data_import_not_backup))
    val result = saved.import(decoded.entries) { id, o ->
        settings.setExerciseIntervals(id, o.duration, o.inhale, o.hold, o.exhale)
    }
    val notes = buildList {
        if (decoded.entries.isEmpty()) add(context.getString(R.string.data_import_nothing))
        if (decoded.invalid > 0) {
            add(context.resources.getQuantityString(R.plurals.data_import_invalid_entries, decoded.invalid, decoded.invalid))
        }
        if (decoded.truncated > 0) add(context.resources.getQuantityString(R.plurals.data_import_truncated, PatternBackup.MAX_PATTERNS, PatternBackup.MAX_PATTERNS))
    }
    Outcome(
        label = context.resources.getQuantityString(R.plurals.data_import_done, result.added, result.added, result.duplicates)
            .takeIf { decoded.entries.isNotEmpty() },
        note = notes.joinToString(" ").ifEmpty { null },
    )
}

/** Liest höchstens [limit] Bytes; null, wenn die Datei größer ist (wird nicht ganz geladen). */
private fun readAtMost(context: Context, uri: Uri, limit: Int): ByteArray? {
    val stream = context.contentResolver.openInputStream(uri) ?: throw IOException()
    return stream.use { input ->
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
