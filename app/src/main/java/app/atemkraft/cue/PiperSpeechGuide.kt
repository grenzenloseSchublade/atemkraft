package app.atemkraft.cue

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import app.atemkraft.cue.tts.VoiceDownloadState
import app.atemkraft.cue.tts.VoiceModelManager
import app.atemkraft.domain.supervisedScope
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Neuronale Sprachanleitung über sherpa-onnx (Piper). Kann zwischen **mehreren installierten**
 * Stimmen (`VoiceCatalog`) umschalten – die aktive Stimme wird per [setActiveVoice] gewählt.
 * Läuft nach dem Download vollständig offline.
 *
 * Das Modell (rund 100 MB nativer Speicher) lädt erst, wenn gesprochen werden soll: [ensureInit]
 * beim Start einer Meditation, und [unload] gibt es danach wieder frei (S-45). Bis dahin meldet
 * [available] nur, ob die aktive Stimme installiert ist – das genügt für Schalter und Einstellungen.
 *
 * Spiegelt die API-Form von [SpeechGuide] (speak/stop/release + [available]) für den
 * `MeditationAudioCoordinator` (`playback/`). Laden, Synthese und Wiedergabe laufen seriell auf
 * einem eigenen Thread – eine Ansage wartet so hinter dem Laden, statt verloren zu gehen; ein
 * Epochen-Zähler bricht laufende/wartende Ausgaben sauber ab.
 */
class PiperSpeechGuide(
    context: Context,
    private val modelManager: VoiceModelManager,
) : SpeechEngine {
    private val appContext = context.applicationContext
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val executor = Executors.newSingleThreadExecutor()
    private val scope = supervisedScope(Dispatchers.Main.immediate)

    private val _available = MutableStateFlow(false)

    /** true, sobald die AKTIVE Stimme installiert ist (geladen wird ihr Modell erst mit [ensureInit]). */
    override val available: StateFlow<Boolean> = _available.asStateFlow()

    @Volatile private var tts: OfflineTts? = null

    @Volatile private var sampleRate: Int = 22050

    @Volatile private var desiredVoiceId: String? = null

    /** Soll das Modell geladen sein? [ensureInit] setzt es, [unload] nimmt es zurück. */
    @Volatile private var wanted = false

    @Volatile private var loadedVoiceId: String? = null

    /** Stimme, deren Modell sich nicht laden ließ: gilt bis zu einem neuen Download als nicht verfügbar. */
    @Volatile private var brokenVoiceId: String? = null

    /** Erhöht bei jedem (Neu-)Aufbau/Teardown: veraltete Build-Ergebnisse werden verworfen. */
    @Volatile private var initEpoch = 0

    /** „Generation“ der Wiedergabe: stop() erhöht ihn und bricht ältere Ausgaben ab. */
    @Volatile private var epoch = 0

    @Volatile private var currentTrack: AudioTrack? = null

    init {
        scope.launch {
            modelManager.states.collect { states ->
                // Verfügbarkeit nachziehen, wenn die aktive Stimme gerade fertig wurde oder entfernt ist.
                val target = desiredVoiceId
                if (target != null) {
                    val installed = states[target] is VoiceDownloadState.Downloaded
                    val expected = installed && target != brokenVoiceId
                    if (expected != _available.value || (!installed && loadedVoiceId == target)) refresh()
                }
            }
        }
    }

    /** Aktive Stimme setzen (null = keine → Fallback). Merkt sie sich nur; das Modell lädt [ensureInit]. */
    fun setActiveVoice(voiceId: String?) {
        if (voiceId == desiredVoiceId) return
        desiredVoiceId = voiceId
        refresh()
    }

    /** Ist [voiceId] die aktuell gewünschte (aktive) Stimme? Quelle für die Lösch-Koordination. */
    fun isDesired(voiceId: String): Boolean = desiredVoiceId == voiceId

    /** Modell der aktiven Stimme laden (falls installiert); idempotent, nicht-blockierend. */
    override fun ensureInit() {
        if (wanted && loadedVoiceId == desiredVoiceId) return
        wanted = true
        refresh()
    }

    /** Modell freigeben (nach der Meditation, bei Speicherdruck); die Wahl der Stimme bleibt. */
    fun unload() {
        if (!wanted && tts == null) return
        wanted = false
        refresh()
    }

    /**
     * Engine für [voiceId] sicher freigeben und danach [onReleased] ausführen – beides auf dem
     * seriellen Executor, sodass eine ggf. gerade laufende generate() zuerst fertig wird. So kann
     * der Aufrufer die Modelldateien erst löschen, wenn die native Engine sie nicht mehr nutzt.
     */
    fun releaseVoiceThen(voiceId: String, onReleased: () -> Unit) {
        if (desiredVoiceId == voiceId) desiredVoiceId = null
        initEpoch++ // ältere Aufträge verfallen, ein Laden dieser Stimme kommt nicht mehr zum Zug
        executor.execute {
            if (loadedVoiceId == voiceId) freeEngine()
            if (brokenVoiceId == voiceId) brokenVoiceId = null
            onReleased()
        }
        refresh() // danach den Wunsch wiederherstellen (Verfügbarkeit, ggf. das Modell einer anderen Stimme)
    }

    /**
     * Zustand auf dem Executor herstellen: Verfügbarkeit der gewünschten Stimme prüfen, ein nicht
     * (mehr) gebrauchtes Modell freigeben und – nur wenn [wanted] – das gewünschte laden.
     */
    private fun refresh() {
        initEpoch++
        val myEpoch = initEpoch
        flush()
        val target = desiredVoiceId
        val load = wanted
        if (target == null) _available.value = false
        executor.execute {
            // Ein späterer Aufruf steht schon in der Schlange und stellt den neueren Wunsch her.
            if (myEpoch != initEpoch) return@execute
            // Datei-Prüfung/Pfade bewusst hier (Executor), nicht auf dem Main-Thread.
            val installed = target != null && modelManager.isDownloaded(target)
            if (!installed && brokenVoiceId == target) brokenVoiceId = null
            if (tts != null && (!load || !installed || loadedVoiceId != target)) freeEngine()
            _available.value = installed && target != brokenVoiceId
            if (!load || target == null || !_available.value || loadedVoiceId == target) return@execute
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
            if (engine == null) {
                // Defektes Modell: nicht bei jedem Start erneut versuchen, die Ansage nimmt das System-TTS.
                brokenVoiceId = target
                _available.value = false
                return@execute
            }
            if (myEpoch != initEpoch) {
                runCatching { engine.release() }
                return@execute
            }
            sampleRate = engine.sampleRate()
            tts = engine
            loadedVoiceId = target
        }
    }

    /** Nur auf dem Executor: geladenes Modell freigeben. */
    private fun freeEngine() {
        tts?.let { runCatching { it.release() } }
        tts = null
        loadedVoiceId = null
    }

    /**
     * Spricht [text], sofern bereit und die Medien-Lautstärke nicht 0 ist (bewusste Stille
     * respektieren). Reiht sich hinter laufende Ansagen ein. Nicht-blockierend.
     */
    override fun speak(text: String) {
        // Lädt das Modell noch, wartet die Ansage auf dem Executor dahinter.
        if (!wanted || !_available.value) return
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
            sr,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
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
                    Thread.sleep(20)
                    waited += 20
                }
                Thread.sleep(60)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            if (myEpoch != epoch) {
                runCatching {
                    track.pause()
                    track.flush()
                }
            }
            runCatching { track.stop() }
            track.release()
            if (currentTrack === track) currentTrack = null
        }
    }

    /** Laufende Ausgabe abbrechen (Engine bleibt bestehen). */
    override fun stop() = flush()

    private fun flush() {
        epoch++
        currentTrack?.let {
            runCatching {
                it.pause()
                it.flush()
            }
        }
    }

    /** Alles freigeben. */
    override fun release() {
        initEpoch++
        flush()
        executor.execute { freeEngine() }
        executor.shutdown()
    }

    companion object {
        /** Ruhige Sprechgeschwindigkeit (<1 = langsamer) für den Meditations-Kontext. */
        private const val SPEED = 0.9f
        private const val CHUNK = 8192
    }
}
