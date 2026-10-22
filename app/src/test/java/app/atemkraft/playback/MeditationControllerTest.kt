package app.atemkraft.playback

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.atemkraft.data.LogbookRepository
import app.atemkraft.data.SettingsRepository
import app.atemkraft.data.local.LogEntryEntity
import app.atemkraft.data.local.LogbookDao
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.domain.SessionTiming
import app.atemkraft.domain.ToneVolume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Ablauf der Meditation in virtueller Zeit (kotlinx-coroutines-test, ohne Robolectric): Countdown,
 * Gongs an der aktiven Zeit, Pausen, Logbuch und Service. Uhr und Dispatcher kommen vom
 * Test-Scheduler; Audio und Service sind Attrappen, die nur mitzählen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MeditationControllerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val audio = CountingAudio()
    private val service = CountingService()
    private val dao = MemoryLogbookDao()

    @After
    fun tearDown() {
        storeScope.cancel()
    }

    private fun TestScope.controller(file: String = "settings.preferences_pb"): MeditationController {
        val store = PreferenceDataStoreFactory.create(scope = storeScope) { File(folder.root, file) }
        return MeditationController(
            settingsRepository = SettingsRepository(store),
            logbook = LogbookRepository(dao),
            audio = audio,
            service = service,
            now = { testScheduler.currentTime },
            dispatcher = StandardTestDispatcher(testScheduler),
        )
    }

    private fun TestScope.advance(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun `feste Dauer - Countdown, Start-Gong, Ende genau nach der Sollzeit, Logbuch, Service`() = runTest {
        val c = controller()
        c.start(MeditationConfig(minutes = 1))
        runCurrent()
        assertEquals(MeditationStatus.PREPARING, c.state.value.status)
        assertEquals(1, service.started)

        advance(COUNTDOWN_MS)
        assertEquals(MeditationStatus.RUNNING, c.state.value.status)
        assertEquals(1, audio.gongs) // Start-Gong nach dem Countdown

        advance(60_000L - 1)
        assertEquals(MeditationStatus.RUNNING, c.state.value.status)
        advance(1)
        val end = c.state.value
        assertEquals(MeditationStatus.FINISHED, end.status)
        assertEquals(60_000L, end.elapsedMs)
        assertEquals(2, audio.gongs) // End-Gong
        assertEquals(listOf(60_000L), dao.rows.map { it.durationMs })

        // Der Service endet erst nach dem Ausklang des End-Gongs.
        assertEquals(0, service.stopped)
        advance(audio.gongTotalMs().toLong() + 500L)
        assertEquals(1, service.stopped)
    }

    @Test
    fun `Intervall-Gongs an der aktiven Zeit - eine Pause verschiebt sie, ohne mitzuzählen`() = runTest {
        val c = controller()
        c.start(MeditationConfig(minutes = 10, gongEveryMin = 2))
        advance(COUNTDOWN_MS + 3 * MINUTE)
        assertEquals(1 + 1, audio.gongs) // Start + 2 min

        c.pause()
        advance(5 * MINUTE)
        assertEquals(MeditationStatus.PAUSED, c.state.value.status)
        assertEquals(2, audio.gongs)

        c.resume()
        advance(MINUTE + SessionTiming.FRAME_MS) // aktiv 4 min (Gong am nächsten Tick)
        assertEquals(3, audio.gongs)
        advance(6 * MINUTE)
        val end = c.state.value
        assertEquals(MeditationStatus.FINISHED, end.status)
        // Start, 2/4/6/8 min, Ende – bei 10 min übernimmt der End-Gong.
        assertEquals(6, audio.gongs)
        // Die Restzeit nach der Pause stammt vom letzten Tick: höchstens ein Frame Abweichung.
        assertEquals(10 * MINUTE.toDouble(), end.elapsedMs.toDouble(), SessionTiming.FRAME_MS.toDouble())
    }

    @Test
    fun `Beenden - offene Sitzung loggt ihre aktive Zeit, Abbruch im Countdown loggt nichts`() = runTest {
        val open = controller()
        open.start(MeditationConfig(mode = MeditationMode.FREE, startEndGong = false))
        advance(COUNTDOWN_MS + 90_000L)
        assertEquals(90_000L, open.state.value.elapsedMs)

        open.end()
        advance(0)
        val end = open.state.value
        assertEquals(MeditationStatus.FINISHED, end.status)
        assertEquals(90_000L, end.elapsedMs)
        assertEquals(listOf(90_000L), dao.rows.map { it.durationMs })
        assertEquals(1, service.stopped)

        val aborted = controller("zweite.preferences_pb")
        aborted.start(MeditationConfig(minutes = 5))
        advance(1_000L)
        aborted.end()
        runCurrent()
        assertEquals(MeditationStatus.IDLE, aborted.state.value.status)
        assertEquals(1, dao.rows.size)
        assertEquals(2, service.stopped)
    }

    private class CountingAudio : MeditationAudio {
        var gongs = 0
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
        override fun gong() {
            gongs++
        }
        override fun speak(text: String) = Unit
        override fun stopSpeech() = Unit
    }

    private class CountingService : ForegroundLink {
        var started = 0
        var stopped = 0
        override fun start() {
            started++
        }
        override fun stop() {
            stopped++
        }
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
        const val MINUTE = 60_000L
    }
}
