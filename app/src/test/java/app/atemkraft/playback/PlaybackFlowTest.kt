package app.atemkraft.playback

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.atemkraft.cue.CueEvent
import app.atemkraft.cue.PhaseAudio
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.FakeLogbookDao
import app.atemkraft.data.LogbookRepository
import app.atemkraft.data.SettingsRepository
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.Feeling
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.domain.SessionTiming
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.UncaughtErrors
import app.atemkraft.ui.log.LogPeriod
import app.atemkraft.ui.log.groupLogEntries
import app.atemkraft.ui.log.recentMinutes
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.random.Random

/**
 * Die Abläufe in virtueller Zeit (kotlinx-coroutines-test, ohne Robolectric, A-03): Uhr und
 * Dispatcher kommen vom Test-Scheduler, Audio, Service und Logbuch sind Attrappen, die mitschreiben.
 * Geprüft wird, was sonst nur das Gerät zeigt: Takt, Pause und Logbuch-Regel der Atemübung und der
 * offenen Meditation, offenes Halten (Wim Hof) bei Fokusverlust und abgezogenem Kopfhörer, die
 * Übergabe „Danach in die Stille“ samt Foreground-Service und das Aufräumen nach einem Fehler im Ablauf;
 * dazu, wie das Logbuch die letzten 7 Tage zählt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackFlowTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val box = BuiltInExercises.all.first { it.id == "box-4-4-4-4" } // 4-4-4-4, je Runde 16 s
    private val wimHof = BuiltInExercises.all.first { it.id == "wim-hof" } // 30 × 3 s, 2 s, offen, 2 + 15 + 2 s
    private val dao = FakeLogbookDao()
    private val holders = Holders()

    /** Fehler aus anderen Tests derselben JVM; ein Test ohne Fehler ändert ihn nicht. */
    private val errorsBefore = UncaughtErrors.last.value

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

    private fun TestScope.meditation(audio: MeditationAudio = RecordingMeditationAudio()) = MeditationController(
        settingsRepository = SettingsRepository(PreferenceDataStoreFactory.create(scope = storeScope) { File(folder.root, "settings.preferences_pb") }),
        logbook = LogbookRepository(dao),
        audio = audio,
        service = holders.link("meditation"),
        now = { testScheduler.currentTime },
        dispatcher = StandardTestDispatcher(testScheduler),
        random = Random(1),
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
        assertEquals(round + round + CueEvent.FINISH, audio.felt)
        // Ein Eintrag, die aktive Zeit genau ohne Pause, alle Runden.
        assertEquals(listOf(2 * ROUND_MS to 2), dao.rows.map { it.durationMs to it.roundsCompleted })
        // Befinden am Ende (Box ist „Beruhigen“): ruhige Reihe, die Antwort steht am Eintrag (SEC-LEGAL-04).
        assertEquals(Feeling.CALM_SCALE, c.state.value.feelingOptions)
        c.answerFeeling(Feeling.CALMER)
        runCurrent()
        assertEquals(Feeling.CALMER.name, dao.rows.single().feeling)

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
        assertEquals(errorsBefore, UncaughtErrors.last.value)
    }

    /**
     * S-46: „Letzte 7 Tage“ heißt überall dasselbe – heute und die sechs Tage davor wie die
     * Wochenpunkte, nicht ab Montag (Gruppe und Minuten aus derselben Definition); die Minuten
     * werden über alle Sitzungen summiert und erst am Ende abgerundet.
     */
    @Test
    fun `Logbuch zählt die letzten 7 Tage wie die Wochenpunkte, Minuten erst am Ende gerundet`() {
        val zone = ZoneOffset.UTC
        val monday = LocalDate.of(2026, 10, 26) // ab Montag gezählt, wäre die Woche heute fast leer
        fun entry(daysAgo: Long, seconds: Long) = SessionLogEntry(
            exerciseId = "resonanz",
            exerciseName = "Resonanz-Atmung",
            family = BreathingFamily.DOWNREGULATE,
            startedAtEpochMs = monday.minusDays(daysAgo).atTime(7, 30).toInstant(zone).toEpochMilli(),
            durationMs = seconds * 1_000,
            roundsCompleted = 10,
        )
        // Je 90 s an vier der sieben Tage: 6 min (je Tag abgerundet wären es 4); vor sieben Tagen zählt nicht mehr.
        val entries = listOf(0L, 1L, 3L, 6L).map { entry(it, 90) } + entry(7, 600)
        assertEquals(6L, recentMinutes(entries, monday, zone))
        val groups = groupLogEntries(entries, monday, zone)
        assertEquals(
            listOf(LogPeriod.Today, LogPeriod.Yesterday, LogPeriod.LastSevenDays, LogPeriod.Month(YearMonth.of(2026, 10))),
            groups.map { it.period },
        )
        assertEquals(listOf(monday.minusDays(3), monday.minusDays(6)), groups[2].days.map { it.date })
    }

    @Test
    fun `Danach in die Stille - Meditation übernimmt den Service, auch über einen stehenden Ende-Screen`() = runTest {
        val meditation = meditation()
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
        assertEquals(errorsBefore, UncaughtErrors.last.value)
    }

    @Test
    fun `Meditation offen - Gong an der aktiven Zeit, Pause zählt nicht, Ende unter und ab einer Minute`() = runTest {
        val audio = RecordingMeditationAudio(speech = true)
        val m = meditation(audio)
        val config = MeditationConfig(mode = MeditationMode.FREE, gongEveryMin = 1, speech = true)

        // Unter einer Minute beendet: zurück in die Auswahl, kein Eintrag, nichts bleibt gehalten.
        m.start(config)
        runCurrent()
        assertTrue("Sprachmodell lädt erst mit dem Start (S-45)", audio.modelLoaded)
        assertNotNull("Kopfhörer überwacht (S-48)", audio.unplugged)
        advance(COUNTDOWN_MS + 30_000L)
        m.end()
        runCurrent()
        assertEquals(MeditationStatus.IDLE, m.state.value.status)
        assertTrue(dao.rows.isEmpty())
        assertFalse("Modell nach dem Abbruch frei", audio.modelLoaded)
        assertNull(audio.unplugged)
        assertTrue(holders.held.isEmpty())

        // Ab einer Minute: 40 s, 10 min Pause, 30 s – der Intervall-Gong kommt bei 60 s aktiver Zeit, nicht in der Pause.
        audio.gongs = 0
        m.start(config)
        advance(COUNTDOWN_MS + 40_000L)
        assertTrue("erste Ansage nach 20–40 s", audio.spoken.isNotEmpty())
        m.pause()
        advance(10 * 60_000L)
        assertEquals(1, audio.gongs) // nur der Start-Gong
        m.resume()
        advance(25_000L)
        assertEquals(2, audio.gongs)
        // Kopfhörer abgezogen: stumm wie mit dem Schalter, die Meditation läuft weiter (S-48).
        audio.unplugged?.invoke()
        assertTrue(m.state.value.muted)
        assertTrue("an den Ton weitergegeben", audio.muted)
        advance(5_000L)
        assertEquals(MeditationStatus.RUNNING, m.state.value.status)
        m.end()
        runCurrent()
        assertEquals(MeditationStatus.FINISHED, m.state.value.status)
        assertEquals(70_000L, m.state.value.elapsedMs)
        assertEquals(listOf(70_000L), dao.rows.map { it.durationMs })
        // Ausklang abgewartet: Service, Modell und Empfänger sind frei.
        advance(7_000L + 500L)
        assertTrue(holders.held.isEmpty())
        assertFalse(audio.modelLoaded)
        assertNull(audio.unplugged)
        assertEquals(errorsBefore, UncaughtErrors.last.value)
    }

    @Test
    fun `Wim Hof - offenes Halten wartet und zählt hoch, abgezogener Kopfhörer pausiert nicht`() = runTest {
        val audio = RecordingBreathAudio()
        val c = breath(audio)
        c.start(wimHof, SessionConfig(rounds = 1))
        advance(COUNTDOWN_MS)
        assertEquals(CueEvent.INHALE, audio.felt.single())
        advance(WIM_HOF_BREATHS_MS + 2_000L)
        assertEquals(SessionStatus.WAITING_FOR_USER, c.state.value.status)
        assertEquals(PhaseType.HOLD_EMPTY, c.state.value.phaseType)
        // Offenes Halten: wartet ohne Zeitlimit und zählt hoch.
        advance(45_000L)
        assertEquals(SessionStatus.WAITING_FOR_USER, c.state.value.status)
        assertEquals(45_000L, c.state.value.elapsedMs)
        c.continueFromUserPaced()
        runCurrent()
        assertEquals(SessionStatus.RUNNING, c.state.value.status)
        // Kopfhörer abgezogen: stumm (sichtbar am Schalter), der Ablauf läuft weiter.
        audio.unplugged()
        advance(19_000L - 1)
        assertEquals(SessionStatus.RUNNING, c.state.value.status)
        assertTrue(c.state.value.muted)
        advance(1)
        assertEquals(SessionStatus.FINISHED, c.state.value.status)
        // Gespürt wird jede Phase (Haptik), auch stumm.
        assertEquals(60 + 5 + 1, audio.felt.size) // 30 Atemzüge, 5 Phasen ums Halten, Abschluss
        val activeMs = WIM_HOF_BREATHS_MS + 2_000L + 45_000L + 19_000L
        assertEquals(listOf(activeMs to 1), dao.rows.map { it.durationMs to it.roundsCompleted })
        assertEquals(errorsBefore, UncaughtErrors.last.value)
    }

    @Test
    fun `Fehler im Ablauf - die Sitzung endet, der Service wird frei, der Fehler wird vermerkt`() = runTest {
        // Atemübung: Der Fehler kommt am Ende (Ausklang des Gongs) – ohne Aufräumen hielte der Service für immer.
        val c = breath(RecordingBreathAudio(tailFails = true))
        c.start(box, SessionConfig(rounds = 1))
        advance(COUNTDOWN_MS + ROUND_MS)
        assertEquals(SessionStatus.IDLE, c.state.value.status)
        assertTrue(holders.held.isEmpty())
        assertEquals("TailBroke", UncaughtErrors.last.value)

        // Meditation: Der Start-Gong scheitert – nicht in RUNNING hängen, Modell und Empfänger frei.
        val audio = RecordingMeditationAudio(speech = true, gongFails = true)
        val m = meditation(audio)
        m.start(MeditationConfig(minutes = 5, speech = true))
        advance(COUNTDOWN_MS)
        assertEquals(MeditationStatus.IDLE, m.state.value.status)
        assertTrue(holders.held.isEmpty())
        assertFalse(audio.modelLoaded)
        assertNull(audio.unplugged)
        assertEquals("GongBroke", UncaughtErrors.last.value)
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

    /**
     * Schreibt die Cues mit ([felt]: die Haptik folgt jedem). Ob ein Cue klingt, regelt der
     * `SessionAudioCoordinator` (R-AUDIO am Gerät), nicht diese Attrappe. [unplugged] ist der Rückruf
     * für den abgezogenen Kopfhörer; mit [tailFails] scheitert das Ende.
     */
    private class RecordingBreathAudio(private val tailFails: Boolean = false) : BreathAudio {
        val felt = mutableListOf<CueEvent>()
        var unplugged: () -> Unit = {}

        override fun bind(
            scope: CoroutineScope,
            status: Flow<SessionStatus>,
            cues: Flow<CueEvent>,
            phaseAudio: Flow<PhaseAudio>,
            muted: Flow<Boolean>,
            onUnplugged: () -> Unit,
        ) {
            unplugged = onUnplugged
            scope.launch { cues.collect { felt += it } }
        }

        override fun finishTailMs(muted: Boolean) = if (tailFails) throw TailBroke() else 0L
    }

    /** Merkt sich Gongs, Ansagen, den Stummschalter, das geladene Sprachmodell und den Kopfhörer-Rückruf. */
    private class RecordingMeditationAudio(speech: Boolean = false, private val gongFails: Boolean = false) : MeditationAudio {
        override val speechAvailable: StateFlow<Boolean> = MutableStateFlow(speech)
        override val piperReady: StateFlow<Boolean> = MutableStateFlow(false)
        var gongs = 0
        val spoken = mutableListOf<String>()
        var modelLoaded = false
        var unplugged: (() -> Unit)? = null
        var muted = false
            private set

        override fun checkSpeech() = Unit

        override fun prepareSpeech() {
            modelLoaded = true
        }

        override fun releaseSpeech() {
            modelLoaded = false
        }

        override fun watchUnplug(onUnplugged: (() -> Unit)?) {
            unplugged = onUnplugged
        }

        override fun setActiveVoice(voiceId: String?) = Unit
        override fun isActiveVoice(voiceId: String) = false
        override fun deleteVoice(voiceId: String) = Unit
        override fun updateVolume(volume: ToneVolume) = Unit
        override fun setGongLong(long: Boolean) = Unit
        override fun gongTotalMs() = 7_000

        override fun setMuted(value: Boolean) {
            muted = value
        }

        override fun abandonFocus() = Unit

        override fun gong() {
            if (gongFails) throw GongBroke()
            gongs++
        }

        override fun silenceGong() = Unit

        override fun speak(text: String) {
            spoken += text
        }

        override fun stopSpeech() = Unit
    }

    private class TailBroke : RuntimeException()

    private class GongBroke : RuntimeException()

    private companion object {
        const val COUNTDOWN_MS = SessionTiming.COUNTDOWN_SECONDS * SessionTiming.COUNTDOWN_STEP_MS
        const val ROUND_MS = 16_000L
        const val PAUSE_AT_MS = 60 * SessionTiming.FRAME_MS
        const val WIM_HOF_BREATHS_MS = 30 * 3_000L
    }
}
