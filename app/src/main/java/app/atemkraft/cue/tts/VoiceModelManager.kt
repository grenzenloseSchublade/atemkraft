package app.atemkraft.cue.tts

import android.content.Context
import app.atemkraft.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** User-Agent des Stimm-Downloads: nur App-Name und Version, keine Gerätedaten. */
private val USER_AGENT = "Atemkraft/${BuildConfig.VERSION_NAME}"

/** Download-Zustand einer einzelnen Stimme. */
sealed interface VoiceDownloadState {
    data object NotDownloaded : VoiceDownloadState
    data class Downloading(
        val fraction: Float,
        val extracting: Boolean = false,
        val indeterminate: Boolean = false,
    ) : VoiceDownloadState
    data object Downloaded : VoiceDownloadState
    data class Failed(val message: String) : VoiceDownloadState
}

/** Ein Stimm-Archiv weicht vom gepinnten Stand ab (Größe, Prüfsumme oder unzulässiger Eintrag). */
class VoiceIntegrityException(message: String) : IOException(message)

/** Absolute Pfade der drei für sherpa-onnx nötigen Bestandteile. */
data class VoiceModelPaths(val model: String, val tokens: String, val dataDir: String)

/**
 * Verwaltet die **mehreren** neuronalen Stimm-Modelle ([VoiceCatalog]): Download bei Bedarf,
 * Entpacken nach [filesDir]/tts, Behalten mehrerer gleichzeitig, einzelnes Entfernen. Jede Stimme
 * hat ihren eigenen Zustand; danach läuft alles offline. App-weit; I/O auf [Dispatchers.IO].
 *
 * Integrität: Jedes Archiv wird beim Laden gegen [VoiceSpec.sizeBytes] und [VoiceSpec.sha256]
 * geprüft (gestreamt, vor dem Entpacken). Installiert wird atomar: Download nach `*.part`,
 * Entpacken in einen Staging-Ordner, erst danach Umbenennen in den Zielordner.
 */
class VoiceModelManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val baseDir = File(appContext.filesDir, "tts")
    private val jobs = mutableMapOf<String, Job>()

    private val _states = MutableStateFlow(
        VoiceCatalog.all.associate { spec ->
            spec.id to if (isComplete(spec)) VoiceDownloadState.Downloaded else VoiceDownloadState.NotDownloaded
        },
    )

    /** Zustand je Stimmen-Id. */
    val states: StateFlow<Map<String, VoiceDownloadState>> = _states.asStateFlow()

    private fun dirOf(spec: VoiceSpec) = File(baseDir, spec.dirName)
    private fun onnxOf(spec: VoiceSpec) = File(dirOf(spec), spec.onnxName)
    private fun tokensOf(spec: VoiceSpec) = File(dirOf(spec), "tokens.txt")
    private fun dataDirOf(spec: VoiceSpec) = File(dirOf(spec), "espeak-ng-data")

    /** Entpack-Ziel vor dem Umbenennen; liegt in [baseDir], damit das Rename atomar bleibt. */
    private fun stagingOf(spec: VoiceSpec) = File(baseDir, ".${spec.dirName}.staging")

    private fun isComplete(spec: VoiceSpec): Boolean = isCompleteIn(dirOf(spec), spec)

    private fun isCompleteIn(dir: File, spec: VoiceSpec): Boolean {
        val onnx = File(dir, spec.onnxName)
        return onnx.isFile && onnx.length() > 0L &&
            File(dir, "tokens.txt").isFile && File(dir, "espeak-ng-data").isDirectory
    }

    fun isDownloaded(voiceId: String): Boolean = VoiceCatalog.byId(voiceId)?.let { isComplete(it) } == true

    /** Lade-Pfade einer Stimme – oder null, solange nicht vollständig. */
    fun paths(voiceId: String): VoiceModelPaths? {
        val spec = VoiceCatalog.byId(voiceId) ?: return null
        if (!isComplete(spec)) return null
        return VoiceModelPaths(onnxOf(spec).absolutePath, tokensOf(spec).absolutePath, dataDirOf(spec).absolutePath)
    }

    private fun setState(id: String, state: VoiceDownloadState) {
        _states.update { it + (id to state) }
    }

    /** Download + Entpacken einer Stimme starten (idempotent, solange bereits läuft/geladen). */
    fun download(voiceId: String) {
        val spec = VoiceCatalog.byId(voiceId) ?: return
        val cur = _states.value[voiceId]
        if (cur is VoiceDownloadState.Downloading || cur is VoiceDownloadState.Downloaded) return
        val previous = jobs[voiceId]
        jobs[voiceId] = scope.launch {
            previous?.cancelAndJoin()
            runDownload(spec)
        }
    }

    /** Laufenden Download einer Stimme abbrechen. */
    fun cancel(voiceId: String) {
        jobs[voiceId]?.cancel()
    }

    /** Eine Stimme wieder entfernen (Speicher freigeben) – deterministisch, abseits des Main-Threads. */
    fun delete(voiceId: String) {
        val spec = VoiceCatalog.byId(voiceId) ?: return
        val previous = jobs[voiceId]
        jobs[voiceId] = scope.launch {
            previous?.cancelAndJoin()
            dirOf(spec).deleteRecursively()
            stagingOf(spec).deleteRecursively()
            setState(voiceId, VoiceDownloadState.NotDownloaded)
        }
    }

    private suspend fun runDownload(spec: VoiceSpec) {
        val part = File(appContext.cacheDir, "${spec.id}-download.tar.bz2.part")
        val archive = File(appContext.cacheDir, "${spec.id}-download.tar.bz2")
        val staging = stagingOf(spec)
        val dir = dirOf(spec)
        fun cleanup() {
            part.delete()
            archive.delete()
            staging.deleteRecursively()
            dir.deleteRecursively()
        }
        try {
            setState(spec.id, VoiceDownloadState.Downloading(0f))
            baseDir.mkdirs()
            cleanup() // sauberer Neuversuch

            downloadVerified(spec, part) { frac ->
                setState(spec.id, VoiceDownloadState.Downloading(frac))
            }
            // Erst nach bestandener Prüfung trägt die Datei ihren endgültigen Namen.
            if (!part.renameTo(archive)) throw IOException("Download konnte nicht abgelegt werden")

            setState(spec.id, VoiceDownloadState.Downloading(1f, extracting = true))
            extractTarBz2(archive, staging)
            archive.delete()

            val extracted = File(staging, spec.dirName)
            if (!isCompleteIn(extracted, spec)) {
                cleanup()
                setState(spec.id, VoiceDownloadState.Failed("Archiv unvollständig"))
                return
            }
            if (!extracted.renameTo(dir)) throw IOException("Stimme konnte nicht installiert werden")
            staging.deleteRecursively()
            setState(spec.id, VoiceDownloadState.Downloaded)
        } catch (c: CancellationException) {
            cleanup()
            setState(spec.id, VoiceDownloadState.NotDownloaded)
            throw c
        } catch (e: Exception) {
            cleanup()
            setState(spec.id, VoiceDownloadState.Failed(e.message ?: "Download fehlgeschlagen"))
        }
    }

    /** Lädt [VoiceSpec.tarUrl] nach [target] und prüft dabei Größe und SHA-256 (siehe [copyVerified]). */
    private suspend fun downloadVerified(
        spec: VoiceSpec,
        target: File,
        onProgress: (Float) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val ctx = coroutineContext
        val conn = (URL(spec.tarUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            // Eigener User-Agent statt des System-Standards („Dalvik/… (Linux; U; Android …;
            // <Gerätemodell> Build/…)“): GitHub erfährt so weder Gerät noch Android-Version
            // (SEC-NET-02, docs/PRIVACY.md).
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            conn.connect()
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            val announced = conn.contentLengthLong
            if (announced > 0 && announced != spec.sizeBytes) {
                throw VoiceIntegrityException(
                    "Unerwartete Download-Größe ($announced statt ${spec.sizeBytes} Bytes)",
                )
            }
            try {
                conn.inputStream.use { input ->
                    target.outputStream().use { out ->
                        copyVerified(
                            input,
                            out,
                            spec.sizeBytes,
                            spec.sha256,
                            checkActive = { ctx.ensureActive() },
                            onBytes = { read ->
                                onProgress((read.toFloat() / spec.sizeBytes).coerceIn(0f, 0.99f))
                            },
                        )
                    }
                }
            } catch (e: Throwable) {
                target.delete() // nie eine ungeprüfte oder abweichende Datei liegen lassen
                throw e
            }
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun extractTarBz2(archive: File, targetDir: File) = withContext(Dispatchers.IO) {
        val ctx = coroutineContext
        extractTarBz2Safely(archive, targetDir) { ctx.ensureActive() }
    }
}

/**
 * Kopiert [input] nach [out] und prüft dabei gestreamt gegen die gepinnten Werte: bricht ab,
 * sobald mehr als [expectedSize] Bytes ankommen; am Ende müssen Größe und SHA-256 exakt passen.
 * Bei Abweichung fliegt eine [VoiceIntegrityException] – das Löschen der Datei macht der Aufrufer.
 */
internal fun copyVerified(
    input: InputStream,
    out: OutputStream,
    expectedSize: Long,
    expectedSha256: String,
    checkActive: () -> Unit = {},
    onBytes: (Long) -> Unit = {},
) {
    val digest = MessageDigest.getInstance("SHA-256")
    val buf = ByteArray(64 * 1024)
    var readTotal = 0L
    while (true) {
        checkActive()
        val n = input.read(buf)
        if (n < 0) break
        readTotal += n
        if (readTotal > expectedSize) {
            throw VoiceIntegrityException("Download größer als erwartet – abgebrochen")
        }
        digest.update(buf, 0, n)
        out.write(buf, 0, n)
        onBytes(readTotal)
    }
    if (readTotal != expectedSize) {
        throw VoiceIntegrityException("Download unvollständig ($readTotal von $expectedSize Bytes)")
    }
    val actual = digest.digest().joinToString("") { "%02x".format(it) }
    if (!actual.equals(expectedSha256, ignoreCase = true)) {
        throw VoiceIntegrityException("Prüfsumme stimmt nicht – Download beschädigt oder verändert")
    }
}

/**
 * Entpackt ein `.tar.bz2` nach [targetDir]. Schutz: kein Pfad außerhalb von [targetDir]
 * (Zip-Slip) und keine Symlinks, Hardlinks oder Gerätedateien – solche Archive werden abgelehnt.
 */
internal fun extractTarBz2Safely(archive: File, targetDir: File, checkActive: () -> Unit = {}) {
    targetDir.mkdirs()
    val canonicalTarget = targetDir.canonicalFile
    TarArchiveInputStream(
        BZip2CompressorInputStream(BufferedInputStream(archive.inputStream())),
    ).use { tar ->
        var entry = tar.nextEntry
        while (entry != null) {
            checkActive()
            if (entry.isSymbolicLink || entry.isLink || entry.isCharacterDevice ||
                entry.isBlockDevice || entry.isFIFO
            ) {
                throw VoiceIntegrityException("Unzulässiger Eintrag im Archiv: ${entry.name}")
            }
            val outFile = File(targetDir, entry.name).canonicalFile
            if (!outFile.path.startsWith(canonicalTarget.path + File.separator)) {
                throw VoiceIntegrityException("Ungültiger Pfad im Archiv: ${entry.name}")
            }
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                outFile.outputStream().use { out -> tar.copyTo(out, 64 * 1024) }
            }
            entry = tar.nextEntry
        }
    }
}
