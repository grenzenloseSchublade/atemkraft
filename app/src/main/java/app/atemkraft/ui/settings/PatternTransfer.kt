package app.atemkraft.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.data.PatternBackup
import app.atemkraft.data.PatternTransferService
import app.atemkraft.data.SavedPatternsRepository
import app.atemkraft.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate

/**
 * Export/Import gespeicherter Muster für die Einstellungen. Erfolg erscheint als neues Label
 * des auslösenden Buttons (MUSTER-02), Hinweise und Fehler als Zeile in der Karte (MUSTER-04).
 */
class PatternTransfer(
    val exportLabel: String? = null,
    val importLabel: String? = null,
    val note: String? = null,
    val export: () -> Unit = {},
    val import: () -> Unit = {},
) {
    companion object {
        /** Ohne Rückmeldung und ohne Wirkung (Vorschau, Tests). */
        val Idle = PatternTransfer()
    }
}

/** Ergebnis eines Vorgangs: neues Button-Label (nur bei Erfolg) und/oder Hinweiszeile. */
private data class Outcome(val label: String? = null, val note: String? = null)

/**
 * Verdrahtet Export/Import über das Storage Access Framework: Der Nutzer wählt Datei und Ort
 * selbst, die App braucht keine Speicher-Berechtigung und behält keinen Zugriff (SEC-STORE-01).
 * Lesen/Schreiben läuft auf [Dispatchers.IO] und [NonCancellable] – ein Zurück während des
 * Imports hinterlässt so kein Muster ohne seine Anpassung. Die Datenlogik (lesen, kodieren,
 * dekodieren, übernehmen) liegt in [PatternTransferService]; hier bleiben Launcher, Zustand und Texte.
 */
@Composable
fun rememberPatternTransfer(saved: SavedPatternsRepository, settings: SettingsRepository): PatternTransfer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val service = remember(saved, settings) { PatternTransferService(saved, settings) }
    val emptyNote = stringResource(R.string.data_export_empty)
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
        scope.launch { show(isExport = true, exportTo(context, uri, service)) }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch { show(isExport = false, importFrom(context, uri, service)) }
    }

    return PatternTransfer(
        exportLabel = exportLabel,
        importLabel = importLabel,
        note = note,
        export = {
            scope.launch {
                if (service.isEmpty()) {
                    show(isExport = true, Outcome(note = emptyNote))
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

private suspend fun exportTo(context: Context, uri: Uri, service: PatternTransferService): Outcome = guarded(context) {
    val count = service.export(System.currentTimeMillis()) { context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException() }
    Outcome(label = context.resources.getQuantityString(R.plurals.data_export_done, count, count))
}

private suspend fun importFrom(context: Context, uri: Uri, service: PatternTransferService): Outcome = guarded(context) {
    when (val outcome = service.import { context.contentResolver.openInputStream(uri) ?: throw IOException() }) {
        PatternTransferService.ImportOutcome.TooLarge -> Outcome(note = context.getString(R.string.data_import_too_large))

        PatternTransferService.ImportOutcome.NotBackup -> Outcome(note = context.getString(R.string.data_import_not_backup))

        is PatternTransferService.ImportOutcome.Done -> {
            val notes = buildList {
                if (outcome.entries == 0) add(context.getString(R.string.data_import_nothing))
                if (outcome.invalid > 0) {
                    add(context.resources.getQuantityString(R.plurals.data_import_invalid_entries, outcome.invalid, outcome.invalid))
                }
                if (outcome.truncated > 0) add(context.resources.getQuantityString(R.plurals.data_import_truncated, PatternBackup.MAX_PATTERNS, PatternBackup.MAX_PATTERNS))
            }
            Outcome(
                label = context.resources.getQuantityString(R.plurals.data_import_done, outcome.added, outcome.added, outcome.duplicates)
                    .takeIf { outcome.entries > 0 },
                note = notes.joinToString(" ").ifEmpty { null },
            )
        }
    }
}
