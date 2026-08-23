package app.atemkraft.cue

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import app.atemkraft.cue.tts.VoiceDownloadState
import app.atemkraft.cue.tts.VoiceModelManager
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Neuronale Sprachanleitung über sherpa-onnx (Piper). Kann zwischen **mehreren installierten**
 * Stimmen ([VoiceCatalog]) umschalten – die aktive Stimme wird per [setActiveVoice] gewählt und
 * geladen. Läuft nach dem Download vollständig offline.
 *
 * Spiegelt die API-Form von [SpeechGuide] (speak/stop/release + [available]) für den
 * [app.atemkraft.ui.meditation.MeditationAudioCoordinator]. Synthese + Wiedergabe seriell auf
 * einem eigenen Thread; ein Epochen-Zähler bricht laufende/wartende Ausgaben sauber ab.
 */
class PiperSpeechGuide(
    context: Context,
    private val modelManager: VoiceModelManager,
) : SpeechEngine {
    private val appContext = context.applicationContext
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val executor = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _available = MutableStateFlow(false)
    /** true, sobald die AKTIVE Stimme geladen und einsatzbereit ist. */
    override val available: StateFlow<Boolean> = _available.asStateFlow()

    @Volatile private var tts: OfflineTts? = null
    @Volatile private var sampleRate: Int = 22050
    @Volatile private var desiredVoiceId: String? = null
    @Volatile private var loadedVoiceId: String? = null
    /** Erhöht bei jedem (Neu-)Aufbau/Teardown: veraltete Build-Ergebnisse werden verworfen. */
    @Volatile private var initEpoch = 0

    /** „Generation" der Wiedergabe: stop() erhöht ihn und bricht ältere Ausgaben ab. */
    @Volatile private var epoch = 0
    @Volatile private var currentTrack: AudioTrack? = null

    init {
        scope.launch {
            modelManager.states.collect { states ->
                // Aktive Stimme neu laden, wenn sie gerade fertig wurde; abbauen, wenn entfernt.
                val target = desiredVoiceId
                if (target != null) {
                    val installed = states[target] is VoiceDownloadState.Downloaded
                    if (installed && loadedVoiceId != target) rebuild()
                    else if (!installed && loadedVoiceId == target) rebuild()
                }
            }
        }
    }

    /** Aktive Stimme setzen (null = keine → Fallback). Lädt sie, sobald installiert. */
    fun setActiveVoice(voiceId: String?) {
        if (voiceId == desiredVoiceId) return
        desiredVoiceId = voiceId
        rebuild()
    }

    /** Ist [voiceId] die aktuell gewünschte (aktive) Stimme? Quelle für die Lösch-Koordination. */
    fun isDesired(voiceId: String): Boolean = desiredVoiceId == voiceId

    /** Sicherstellen, dass die aktive Stimme (falls installiert) geladen ist. */
    override fun ensureInit() {
        if (loadedVoiceId != desiredVoiceId) rebuild()
    }

    /**
     * Engine für [voiceId] sicher freigeben und danach [onReleased] ausführen – beides auf dem
     * seriellen Executor, sodass eine ggf. gerade laufende generate() zuerst fertig wird. So kann
     * der Aufrufer die Modelldateien erst löschen, wenn die native Engine sie nicht mehr nutzt.
     */
    fun releaseVoiceThen(voiceId: String, onReleased: () -> Unit) {
        if (desiredVoiceId == voiceId) desiredVoiceId = null
        initEpoch++
        flush()
        _available.value = false
        executor.execute {
            if (loadedVoiceId == voiceId) {
                tts?.let { runCatching { it.release() } }
                tts = null
                loadedVoiceId = null
            }
            onReleased()
        }
    }

    /** Aktuelle Engine abbauen und – falls die gewünschte Stimme installiert ist – neu aufbauen. */
    private fun rebuild() {
        initEpoch++
        val myEpoch = initEpoch
        flush()
        _available.value = false
        val target = desiredVoiceId
        executor.execute {
            tts?.let { runCatching { it.release() } }
            tts = null
            loadedVoiceId = null
            // Datei-Prüfung/Pfade bewusst hier (Executor), nicht auf dem Main-Thread.
            if (target == null || myEpoch != initEpoch || !modelManager.isDownloaded(target)) return@execute
            val paths = modelManager.paths(target) ?: return@execute
            val engine = runCatching {
                val vits = OfflineTtsVitsModelConfig(
                    model = paths.model,
                    tokens = paths.tokens,
                    dataDir = paths.dataDir,
                )
                OfflineTts(
                    assetManager = null,
                    config = OfflineTtsConfig(model = OfflineTtsModelConfig(vits = vits, numThreads = 2)),
                )
            }.getOrNull()
            if (engine == null || myEpoch != initEpoch) {
                engine?.let { runCatching { it.release() } }
                return@execute
            }
            sampleRate = engine.sampleRate()
            tts = engine
            loadedVoiceId = target
            _available.value = true
        }
    }

    /**
     * Spricht [text], sofern bereit und die Medien-Lautstärke nicht 0 ist (bewusste Stille
     * respektieren). Reiht sich hinter laufende Ansagen ein. Nicht-blockierend.
     */
    override fun speak(text: String) {
        if (tts == null) return
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) return
        enqueue(text)
    }

    private fun enqueue(text: String) {
        val myEpoch = epoch
        executor.execute {
            if (myEpoch != epoch) return@execute
            val engine = tts ?: return@execute
            val audio = runCatching { engine.generate(text = text, sid = 0, speed = SPEED) }
                .getOrNull() ?: return@execute
            if (myEpoch != epoch) return@execute
            play(audio.samples, audio.sampleRate, myEpoch)
        }
    }

    private fun play(samples: FloatArray, sr: Int, myEpoch: Int) {
        if (samples.isEmpty()) return
        val minBuf = AudioTrack.getMinBufferSize(
            sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT,
        ).coerceAtLeast(4096)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sr)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        currentTrack = track
        try {
            track.play()
            var offset = 0
            while (offset < samples.size) {
                if (myEpoch != epoch) break
                val chunk = minOf(CHUNK, samples.size - offset)
                val written = track.write(samples, offset, chunk, AudioTrack.WRITE_BLOCKING)
                if (written <= 0) break
                offset += written
            }
            // Warten, bis der Track ALLE Frames abgespielt hat (per Playback-Head), damit auf
            // latenzbehafteten Ausgängen (Bluetooth) die letzte Silbe nicht abreißt.
            if (myEpoch == epoch) {
                val totalFrames = samples.size
                val maxWaitMs = totalFrames * 1000L / sr + 500L
                var waited = 0L
                while (myEpoch == epoch && track.playbackHeadPosition < totalFrames && waited < maxWaitMs) {
                    Thread.sleep(20); waited += 20
                }
                Thread.sleep(60)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            if (myEpoch != epoch) runCatching { track.pause(); track.flush() }
            runCatching { track.stop() }
            track.release()
            if (currentTrack === track) currentTrack = null
        }
    }

    /** Laufende Ausgabe abbrechen (Engine bleibt bestehen). */
    override fun stop() = flush()

    private fun flush() {
        epoch++
        currentTrack?.let { runCatching { it.pause(); it.flush() } }
    }

    /** Alles freigeben. */
    override fun release() {
        initEpoch++
        flush()
        executor.execute {
            tts?.let { runCatching { it.release() } }
            tts = null
        }
        executor.shutdown()
    }

    companion object {
        /** Ruhige Sprechgeschwindigkeit (<1 = langsamer) für den Meditations-Kontext. */
        private const val SPEED = 0.9f
        private const val CHUNK = 8192
    }
}
