package app.atemkraft.cue.tts

import android.content.Context
import app.atemkraft.BuildConfig
import app.atemkraft.domain.supervisedScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import javax.net.ssl.SSLException

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
    data class Failed(val reason: VoiceFailure) : VoiceDownloadState
}

/**
 * Warum das Laden einer Stimme scheiterte – grob genug für einen Satz in der Liste („kein Netz“,
 * „zu wenig Speicher“, „Datei beschädigt“), ohne Exception-Text in der UI (MUSTER-04, S-34).
 */
enum class VoiceFailure {
    /** Keine Verbindung, Zeitüberschreitung oder Serverfehler: später erneut versuchen. */
    NETWORK,

    /** Kein Platz mehr auf dem Gerät. */
    STORAGE,

    /** Größe, Prüfsumme oder Archiv stimmen nicht: erneut laden. */
    DAMAGED,

    /** Alles andere. */
    OTHER,
    ;

    companion object {
        /** Ordnet einen Fehler beim Laden einer Ursache zu. */
        fun of(e: Throwable): VoiceFailure = when {
            e is VoiceIntegrityException -> DAMAGED
            e.isOutOfSpace() -> STORAGE
            e is UnknownHostException || e is SocketException || e is SocketTimeoutException || e is SSLException -> NETWORK
            e is HttpStatusException -> NETWORK
            else -> OTHER
        }

        /** Voller Speicher meldet sich als IOException mit ENOSPC („No space left on device“). */
        private fun Throwable.isOutOfSpace(): Boolean = generateSequence(this) { it.cause }.any { cause ->
            val text = cause.message.orEmpty()
            text.contains("ENOSPC") || text.contains("No space left", ignoreCase = true)
        }
    }
}

/** Der Server antwortete nicht mit 2xx. */
class HttpStatusException(code: Int) : IOException("HTTP $code")

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
    private val scope = supervisedScope(Dispatchers.IO)

    private val baseDir = File(appContext.filesDir, "tts")

    /**
     * Laufende Download-/Lösch-Aufträge je Stimme. Geschrieben vom Main-Thread ([download]) und vom
     * Engine-Thread der Sprache ([delete] nach dem Freigeben der Stimme): nur unter dieser Sperre.
     */
    private val jobs = mutableMapOf<String, Job>()

    private val _states = MutableStateFlow(
        VoiceCatalog.all.associate { spec ->
            spec.id to if (isComplete(spec)) VoiceDownloadState.Downloaded else VoiceDownloadState.NotDownloaded
        },
    )

    /** Zustand je Stimmen-Id. */
    val states: StateFlow<Map<String, VoiceDownloadState>> = _states.asStateFlow()

    /**
     * Teil-Downloads liegen in [baseDir]/.download statt im Cache: Android darf `cacheDir` bei
     * Speichermangel mitten im Download leeren. Was ein beendeter Prozess dort liegen ließ, räumt
     * [startupCleanup] weg, bevor ein neuer Download beginnt.
     */
    private val downloadDir = File(baseDir, ".download")

    /**
     * Aufräumen beim Start: Modelle von Stimmen, die nicht mehr im Katalog stehen (GLaDOS bis
     * 1.5.1, ca. 110 MB), kann niemand mehr wählen oder löschen; dazu Reste abgebrochener
     * Downloads. Jeder Download wartet darauf ([download]).
     */
    private val startupCleanup: Job = scope.launch {
        deleteOrphanVoiceDirs(baseDir, VoiceCatalog.all)
        downloadDir.deleteRecursively()
        // Bis 2.5.0 lagen Teil-Downloads im Cache.
        appContext.cacheDir.listFiles { f -> f.name.contains("-download.tar.bz2") }?.forEach { it.delete() }
    }

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
        synchronized(jobs) {
            val previous = jobs[voiceId]
            jobs[voiceId] = scope.launch {
                startupCleanup.join()
                previous?.cancelAndJoin()
                runDownload(spec)
            }
        }
    }

    /** Laufenden Download einer Stimme abbrechen. */
    fun cancel(voiceId: String) {
        synchronized(jobs) { jobs[voiceId] }?.cancel()
    }

    /** Eine Stimme wieder entfernen (Speicher freigeben) – deterministisch, abseits des Main-Threads. */
    fun delete(voiceId: String) {
        val spec = VoiceCatalog.byId(voiceId) ?: return
        synchronized(jobs) {
            val previous = jobs[voiceId]
            jobs[voiceId] = scope.launch {
                previous?.cancelAndJoin()
                dirOf(spec).deleteRecursively()
                stagingOf(spec).deleteRecursively()
                setState(voiceId, VoiceDownloadState.NotDownloaded)
            }
        }
    }

    private suspend fun runDownload(spec: VoiceSpec) {
        val part = File(downloadDir, "${spec.id}.tar.bz2.part")
        val archive = File(downloadDir, "${spec.id}.tar.bz2")
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
            downloadDir.mkdirs()
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
                setState(spec.id, VoiceDownloadState.Failed(VoiceFailure.DAMAGED))
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
            setState(spec.id, VoiceDownloadState.Failed(VoiceFailure.of(e)))
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
            if (conn.responseCode !in 200..299) throw HttpStatusException(conn.responseCode)
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
        extractTarBz2Safely(archive, targetDir, checkActive = { ctx.ensureActive() })
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
 * Obergrenzen beim Entpacken (SEC-NET-04). Die Archive des Katalogs haben rund 400 Einträge
 * (Modell, `tokens.txt`, `espeak-ng-data`) und entpackt höchstens etwa 130 MB (Thorsten); die
 * Grenzen lassen reichlich Luft und stoppen trotzdem eine Archiv-Bombe, falls der Hash-Pin je
 * versagte.
 */
internal const val MAX_ARCHIVE_ENTRIES = 1_000
internal const val MAX_ARCHIVE_BYTES = 300L * 1024 * 1024

/**
 * Entpackt ein `.tar.bz2` nach [targetDir]. Schutz: kein Pfad außerhalb von [targetDir]
 * (Zip-Slip), keine Symlinks, Hardlinks oder Gerätedateien, höchstens [maxEntries] Einträge und
 * [maxBytes] entpackte Bytes (gezählt wird, was wirklich geschrieben wird, nicht die Angabe im
 * Tar-Kopf) – sonst wird das Archiv abgelehnt; das Aufräumen macht der Aufrufer.
 */
internal fun extractTarBz2Safely(
    archive: File,
    targetDir: File,
    checkActive: () -> Unit = {},
    maxEntries: Int = MAX_ARCHIVE_ENTRIES,
    maxBytes: Long = MAX_ARCHIVE_BYTES,
) {
    targetDir.mkdirs()
    val canonicalTarget = targetDir.canonicalFile
    var entries = 0
    var written = 0L
    val buf = ByteArray(64 * 1024)
    TarArchiveInputStream(
        BZip2CompressorInputStream(BufferedInputStream(archive.inputStream())),
    ).use { tar ->
        var entry = tar.nextEntry
        while (entry != null) {
            checkActive()
            if (++entries > maxEntries) {
                throw VoiceIntegrityException("Archiv hat mehr als $maxEntries Einträge")
            }
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
                outFile.outputStream().use { out ->
                    while (true) {
                        val n = tar.read(buf)
                        if (n < 0) break
                        written += n
                        if (written > maxBytes) {
                            throw VoiceIntegrityException("Archiv entpackt größer als ${maxBytes / (1024 * 1024)} MB")
                        }
                        out.write(buf, 0, n)
                    }
                }
            }
            entry = tar.nextEntry
        }
    }
}

/** Namen, die ein Stimmordner in `files/tts` haben kann: Zielordner oder Staging-Ordner (Piper-Schema). */
private val VOICE_DIR_NAME = Regex("""^vits-piper-[A-Za-z0-9_-]+$|^\.vits-piper-[A-Za-z0-9_-]+\.staging$""")

/**
 * Löscht in [baseDir] die Stimmordner, die zu keiner Stimme aus [catalog] gehören, und liefert
 * ihre Namen. Vorsichtig: nur direkte Unterordner mit dem Namensmuster eines Stimm- oder
 * Staging-Ordners; Symlinks werden weder gelöscht noch verfolgt, auch nicht innerhalb eines
 * Ordners (dort wird nur der Link selbst entfernt). Fehler lassen den Ordner stehen.
 */
internal fun deleteOrphanVoiceDirs(baseDir: File, catalog: List<VoiceSpec>): List<String> {
    if (Files.isSymbolicLink(baseDir.toPath())) return emptyList()
    val known = catalog.flatMap { listOf(it.dirName, ".${it.dirName}.staging") }.toSet()
    val children = baseDir.listFiles() ?: return emptyList()
    return children
        .filter { it.name !in known && VOICE_DIR_NAME.matches(it.name) }
        .filter { Files.isDirectory(it.toPath(), LinkOption.NOFOLLOW_LINKS) }
        .mapNotNull { dir -> dir.name.takeIf { deleteTreeWithoutFollowingLinks(dir.toPath()) } }
}

/** Löscht [root] samt Inhalt; Symlinks darin werden als Links gelöscht, nie ihr Ziel. */
private fun deleteTreeWithoutFollowingLinks(root: Path): Boolean = try {
    // walkFileTree folgt ohne FOLLOW_LINKS keinem Link: ein Link kommt als Datei bei visitFile an.
    Files.walkFileTree(
        root,
        object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                if (exc != null) throw exc
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        },
    )
    true
} catch (e: IOException) {
    false
}
