package app.atemkraft.playback

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.atemkraft.cue.CueEvent
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.LogbookRepository
import app.atemkraft.data.SettingsRepository
import app.atemkraft.data.local.LogEntryEntity
import app.atemkraft.data.local.LogbookDao
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.SessionTiming
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.UncaughtErrors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Die Abläufe in virtueller Zeit (kotlinx-coroutines-test, ohne Robolectric, A-03): Uhr und
 * Dispatcher kommen vom Test-Scheduler, Audio, Service und Logbuch sind Attrappen, die mitschreiben.
 * Zwei Tests für das, was sonst nur das Gerät zeigt: Takt, Pause und Logbuch-Regel der Atemübung
 * sowie die Übergabe „Danach in die Stille“ samt Foreground-Service.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackFlowTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val box = BuiltInExercises.all.first { it.id == "box-4-4-4-4" } // 4-4-4-4, je Runde 16 s
    private val dao = MemoryLogbookDao()
    private val holders = Holders()

    @After
    fun tearDown() {
        storeScope.cancel()
    }

    private fun TestScope.breath(audio: BreathAudio = RecordingBreathAudio()) = BreathSessionController(
        audio = audio,
        logbook = LogbookRepository(dao),
        service = holders.link("breath"),
        now = { testScheduler.currentTime },
        dispatcher = StandardTestDispatcher(testScheduler),
    )

    private fun TestScope.advance(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun `Atemübung - Phasenfolge, Pause zählt nicht, Logbuch am Ende und beim Beenden ab einer Minute`() = runTest {
        val audio = RecordingBreathAudio()
        val c = breath(audio)
        c.start(box, SessionConfig(rounds = 2))
        runCurrent()
        assertEquals(SessionStatus.PREPARING, c.state.value.status)

        // Pause mitten in der ersten Einatmung (auf einem Frame-Tick, der die Restzeit setzt): Die
        // Restzeit bleibt, die Pause zählt nicht mit.
        advance(COUNTDOWN_MS + PAUSE_AT_MS)
        c.pause()
        advance(30_000L)
        assertEquals(SessionStatus.PAUSED, c.state.value.status)
        c.resume()
        advance(2 * ROUND_MS - PAUSE_AT_MS - 1)
        assertEquals(SessionStatus.RUNNING, c.state.value.status)
        advance(1)
        assertEquals(SessionStatus.FINISHED, c.state.value.status)
        val round = listOf(CueEvent.INHALE, CueEvent.HOLD, CueEvent.EXHALE, CueEvent.HOLD)
        assertEquals(round + round + CueEvent.FINISH, audio.cues)
        // Ein Eintrag, die aktive Zeit genau ohne Pause, alle Runden.
        assertEquals(listOf(2 * ROUND_MS to 2), dao.rows.map { it.durationMs to it.roundsCompleted })

        // Früh beendet ab einer Minute: echte Dauer, nur die vollständigen Runden (70 s = 4 Runden + 6 s).
        c.start(box, SessionConfig(rounds = 8))
        advance(COUNTDOWN_MS + 70_000L)
        c.stop()
        runCurrent()
        assertEquals(70_000L to 4, dao.rows.last().let { it.durationMs to it.roundsCompleted })

        // Darunter ein Fehlstart: kein Eintrag, auch nicht beim Ersetzen durch einen neuen Start.
        c.start(box)
        advance(COUNTDOWN_MS + 30_000L)
        c.start(box)
        advance(COUNTDOWN_MS + 10_000L)
        c.stop()
        runCurrent()
        assertEquals(2, dao.rows.size)
        assertTrue("Service am Ende frei", holders.held.isEmpty())
        assertNull(UncaughtErrors.last.value)
    }

    @Test
    fun `Danach in die Stille - Meditation übernimmt den Service, auch über einen stehenden Ende-Screen`() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = storeScope) { File(folder.root, "settings.preferences_pb") }
        val meditation = MeditationController(
            settingsRepository = SettingsRepository(store),
            logbook = LogbookRepository(dao),
            audio = SilentMeditationAudio(),
            service = holders.link("meditation"),
            now = { testScheduler.currentTime },
            dispatcher = StandardTestDispatcher(testScheduler),
        )
        val breath = breath()
        val silence = SilenceAfterBreath(
            breath = breath,
            meditation = meditation,
            meditationConfig = flowOf(MeditationConfig(minutes = 5, startEndGong = false)),
            canStartNow = { true },
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        // Eine Meditation läuft aus und bleibt am Ende-Screen stehen (z. B. minimiert).
        meditation.start(MeditationConfig(minutes = 1, startEndGong = false))
        advance(COUNTDOWN_MS + 60_000L)
        assertEquals(MeditationStatus.FINISHED, meditation.state.value.status)

        breath.start(box, SessionConfig(rounds = 1), silenceAfter = true)
        holders.droppedToZero = false
        advance(COUNTDOWN_MS + ROUND_MS)

        // Die neue Meditation läuft, die Atemübung ist verworfen, der Service war nie frei.
        assertEquals(MeditationStatus.PREPARING, meditation.state.value.status)
        assertEquals(SessionStatus.IDLE, breath.state.value.status)
        assertEquals(setOf("meditation"), holders.held)
        assertFalse("Service zwischendurch freigegeben", holders.droppedToZero)
        assertTrue(silence.consumeHandover())
        // Im Logbuch: die Meditation (1 min) und die Atemübung (16 s, von selbst zu Ende).
        assertEquals(listOf(60_000L, ROUND_MS), dao.rows.map { it.durationMs })
        assertNull(UncaughtErrors.last.value)
    }

    /** Halter-Menge wie [PlaybackServiceLinks]: merkt sich, ob der Service zwischendurch frei war. */
    private class Holders {
        val held = mutableSetOf<String>()
        var droppedToZero = false

        fun link(owner: String): ForegroundLink = object : ForegroundLink {
            override fun start() {
                held += owner
            }

            override fun stop() {
                if (held.remove(owner) && held.isEmpty()) droppedToZero = true
            }
        }
    }

    private class RecordingBreathAudio : BreathAudio {
        val cues = mutableListOf<CueEvent>()

        override fun bind(
            scope: CoroutineScope,
            status: Flow<SessionStatus>,
            cues: Flow<CueEvent>,
            phaseAudio: Flow<PhaseAudio>,
            muted: Flow<Boolean>,
        ) {
            scope.launch { cues.collect { this@RecordingBreathAudio.cues += it } }
        }

        override fun finishTailMs(muted: Boolean) = 0L
    }

    private class SilentMeditationAudio : MeditationAudio {
        override val speechAvailable: StateFlow<Boolean> = MutableStateFlow(false)
        override val piperReady: StateFlow<Boolean> = MutableStateFlow(false)
        override fun prepareSpeech() = Unit
        override fun setActiveVoice(voiceId: String?) = Unit
        override fun isActiveVoice(voiceId: String) = false
        override fun deleteVoice(voiceId: String) = Unit
        override fun updateVolume(volume: ToneVolume) = Unit
        override fun setGongLong(long: Boolean) = Unit
        override fun gongTotalMs() = 7_000
        override fun setMuted(value: Boolean) = Unit
        override fun requestFocus() = Unit
        override fun abandonFocus() = Unit
        override fun gong() = Unit
        override fun speak(text: String) = Unit
        override fun stopSpeech() = Unit
    }

    private class MemoryLogbookDao : LogbookDao {
        val rows = mutableListOf<LogEntryEntity>()

        override suspend fun insert(entry: LogEntryEntity) {
            rows += entry
        }

        override fun all(): Flow<List<LogEntryEntity>> = MutableStateFlow(rows.toList())

        override suspend fun clear() = rows.clear()
    }

    private companion object {
        const val COUNTDOWN_MS = SessionTiming.COUNTDOWN_SECONDS * SessionTiming.COUNTDOWN_STEP_MS
        const val ROUND_MS = 16_000L
        const val PAUSE_AT_MS = 60 * SessionTiming.FRAME_MS
    }
}
