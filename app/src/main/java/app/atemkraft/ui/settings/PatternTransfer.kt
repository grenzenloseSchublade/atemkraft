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
import app.atemkraft.R
import app.atemkraft.data.DecodedBackup
import app.atemkraft.data.LogbookRepository
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
 * Sicherung „alles“ für die Einstellungen: Export und Import mit Vorschau. Erfolg erscheint als
 * neues Label des auslösenden Buttons (MUSTER-02), Hinweise und Fehler als Zeile in der Karte
 * (MUSTER-04). [preview] ist eine gelesene Datei, die noch auf „Übernehmen“ oder „Abbrechen“ wartet
 * ([confirm], [cancel]); [withSettings] sagt, ob dabei auch die Einstellungen ersetzt werden
 * ([toggleSettings], Standard nein).
 */
class PatternTransfer(
    val exportLabel: String? = null,
    val importLabel: String? = null,
    val note: String? = null,
    val preview: BackupPreview? = null,
    val withSettings: Boolean = false,
    val export: () -> Unit = {},
    val import: () -> Unit = {},
    val confirm: () -> Unit = {},
    val cancel: () -> Unit = {},
    val toggleSettings: (Boolean) -> Unit = {},
) {
    companion object {
        /** Ohne Rückmeldung und ohne Wirkung (Vorschau, Tests). */
        val Idle = PatternTransfer()
    }
}

/** Was eine gelesene Sicherung enthält (Vorschau vor dem Übernehmen). */
data class BackupPreview(val patterns: Int, val logbook: Int, val marked: Int, val hasSettings: Boolean)

/** Ergebnis eines Vorgangs: neues Button-Label (nur bei Erfolg) und/oder Hinweiszeile. */
private data class Outcome(val label: String? = null, val note: String? = null)

/**
 * Verdrahtet Export und Import über das Storage Access Framework: Der Nutzer wählt Datei und Ort
 * selbst, die App braucht keine Speicher-Berechtigung und behält keinen Zugriff (SEC-STORE-01).
 * Lesen und Schreiben laufen auf [Dispatchers.IO] und [NonCancellable] – ein Zurück während des
 * Übernehmens hinterlässt so kein Muster ohne seine Anpassung. Die gelesene Datei wartet nur im
 * Speicher dieses Bildschirms auf die Bestätigung; nach dem Drehen wählt man sie neu. Die
 * Datenlogik liegt in [PatternTransferService]; hier bleiben Launcher, Zustand und Texte.
 */
@Composable
fun rememberPatternTransfer(saved: SavedPatternsRepository, settings: SettingsRepository, logbook: LogbookRepository): PatternTransfer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val service = remember(saved, settings, logbook) { PatternTransferService(saved, settings, logbook) }
    var exportLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var importLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<DecodedBackup?>(null) }
    var withSettings by remember { mutableStateOf(false) }

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
        scope.launch {
            val (decoded, outcome) = readFrom(context, uri, service)
            pending = decoded
            withSettings = false
            show(isExport = false, outcome)
        }
    }

    return PatternTransfer(
        exportLabel = exportLabel,
        importLabel = importLabel,
        note = note,
        preview = pending?.let { BackupPreview(it.entries.size, it.logbook.size, it.marked.size, it.settings != null) },
        withSettings = withSettings,
        export = { createLauncher.launch("atemkraft-sicherung-${LocalDate.now()}.json") },
        // Manche Dateimanager melden JSON als octet-stream oder text/plain.
        import = { openLauncher.launch(arrayOf(MIME_JSON, "text/plain", "application/octet-stream")) },
        confirm = {
            val decoded = pending
            if (decoded != null) {
                pending = null
                scope.launch { show(isExport = false, applyBackup(context, decoded, withSettings, service)) }
            }
        },
        cancel = {
            pending = null
            note = null
        },
        toggleSettings = { withSettings = it },
    )
}

private const val MIME_JSON = "application/json"

/** Gemeinsamer Rahmen: IO-Thread, nicht abbrechbar, jeder Fehler als Hinweis statt Absturz. */
private suspend fun <T> guarded(context: Context, failed: T, block: suspend () -> T): T = withContext(Dispatchers.IO + NonCancellable) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        failed
    } catch (_: RuntimeException) {
        // Fremde DocumentsProvider werfen auch IllegalArgument-/UnsupportedOperation-
        // Exceptions, Room bei vollem Speicher SQLiteFullException. Bewusst ohne Log und
        // ohne e.message (SEC-PRIV-03, MUSTER-04).
        failed
    }
}

private fun ioError(context: Context) = Outcome(note = context.getString(R.string.data_io_error))

private suspend fun exportTo(context: Context, uri: Uri, service: PatternTransferService): Outcome = guarded(context, ioError(context)) {
    service.export(System.currentTimeMillis()) { context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException() }
    Outcome(label = context.getString(R.string.data_export_done))
}

/** Liest und prüft die Datei; die Vorschau (oder null) und was dazu gemeldet wird. */
private suspend fun readFrom(context: Context, uri: Uri, service: PatternTransferService): Pair<DecodedBackup?, Outcome> = guarded(context, null to ioError(context)) {
    when (val outcome = service.read { context.contentResolver.openInputStream(uri) ?: throw IOException() }) {
        PatternTransferService.ReadOutcome.TooLarge -> null to Outcome(note = context.getString(R.string.data_import_too_large))

        PatternTransferService.ReadOutcome.NotBackup -> null to Outcome(note = context.getString(R.string.data_import_not_backup))

        is PatternTransferService.ReadOutcome.Preview -> {
            val decoded = outcome.decoded
            val res = context.resources
            val notes = buildList {
                if (decoded.isEmpty) add(context.getString(R.string.data_import_nothing))
                if (decoded.invalid > 0) add(res.getQuantityString(R.plurals.data_import_invalid_entries, decoded.invalid, decoded.invalid))
                if (decoded.truncated > 0) add(res.getQuantityString(R.plurals.data_import_truncated, PatternBackup.MAX_PATTERNS, PatternBackup.MAX_PATTERNS))
                if (decoded.logTruncated > 0) {
                    add(res.getQuantityString(R.plurals.data_import_log_truncated, PatternBackup.MAX_LOG_ENTRIES, PatternBackup.MAX_LOG_ENTRIES))
                }
            }
            decoded.takeUnless { it.isEmpty } to Outcome(note = notes.joinToString(" ").ifEmpty { null })
        }
    }
}

private suspend fun applyBackup(context: Context, decoded: DecodedBackup, withSettings: Boolean, service: PatternTransferService): Outcome = guarded(context, ioError(context)) {
    val result = service.apply(decoded, withSettings)
    Outcome(
        label = context.resources.getQuantityString(R.plurals.data_import_done, result.added, result.added, result.duplicates),
        note = context.getString(R.string.data_import_settings_done).takeIf { result.settingsApplied },
    )
}
