package app.atemkraft.cue.tts

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
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
import java.net.HttpURLConnection
import java.net.URL

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

/** Absolute Pfade der drei für sherpa-onnx nötigen Bestandteile. */
data class VoiceModelPaths(val model: String, val tokens: String, val dataDir: String)

/**
 * Verwaltet die **mehreren** neuronalen Stimm-Modelle ([VoiceCatalog]): Download bei Bedarf,
 * Entpacken nach [filesDir]/tts, Behalten mehrerer gleichzeitig, einzelnes Entfernen. Jede Stimme
 * hat ihren eigenen Zustand; danach läuft alles offline. App-weit; I/O auf [Dispatchers.IO].
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

    private fun isComplete(spec: VoiceSpec): Boolean =
        onnxOf(spec).isFile && onnxOf(spec).length() > 0L &&
            tokensOf(spec).isFile && dataDirOf(spec).isDirectory

    fun isDownloaded(voiceId: String): Boolean =
        VoiceCatalog.byId(voiceId)?.let { isComplete(it) } == true

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
            setState(voiceId, VoiceDownloadState.NotDownloaded)
        }
    }

    private suspend fun runDownload(spec: VoiceSpec) {
        val tmp = File(appContext.cacheDir, "${spec.id}-download.tar.bz2")
        val dir = dirOf(spec)
        try {
            setState(spec.id, VoiceDownloadState.Downloading(0f))
            baseDir.mkdirs()
            dir.deleteRecursively() // sauberer Neuversuch
            tmp.delete()

            downloadTo(spec.tarUrl, tmp) { frac, indeterminate ->
                setState(spec.id, VoiceDownloadState.Downloading(frac, indeterminate = indeterminate))
            }

            setState(spec.id, VoiceDownloadState.Downloading(1f, extracting = true))
            extractTarBz2(tmp, baseDir)
            tmp.delete()

            setState(
                spec.id,
                if (isComplete(spec)) {
                    VoiceDownloadState.Downloaded
                } else {
                    dir.deleteRecursively()
                    VoiceDownloadState.Failed("Archiv unvollständig")
                },
            )
        } catch (c: CancellationException) {
            tmp.delete()
            dir.deleteRecursively()
            setState(spec.id, VoiceDownloadState.NotDownloaded)
            throw c
        } catch (e: Exception) {
            tmp.delete()
            dir.deleteRecursively()
            setState(spec.id, VoiceDownloadState.Failed(e.message ?: "Download fehlgeschlagen"))
        }
    }

    private suspend fun downloadTo(
        url: String,
        target: File,
        onProgress: (Float, Boolean) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        try {
            conn.connect()
            if (conn.responseCode !in 200..299) throw java.io.IOException("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            if (total <= 0) onProgress(0f, true)
            conn.inputStream.use { input ->
                target.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var readTotal = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        readTotal += n
                        if (total > 0) onProgress((readTotal.toFloat() / total).coerceIn(0f, 0.99f), false)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun extractTarBz2(archive: File, targetDir: File) = withContext(Dispatchers.IO) {
        val canonicalTarget = targetDir.canonicalFile
        TarArchiveInputStream(
            BZip2CompressorInputStream(BufferedInputStream(archive.inputStream())),
        ).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                currentCoroutineContext().ensureActive()
                val outFile = File(targetDir, entry.name).canonicalFile
                if (!outFile.path.startsWith(canonicalTarget.path + File.separator)) {
                    throw java.io.IOException("Ungültiger Pfad im Archiv: ${entry.name}")
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
}
