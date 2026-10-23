package app.atemkraft.ui.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import app.atemkraft.R
import app.atemkraft.cue.tts.VoiceCatalog
import app.atemkraft.cue.tts.VoiceDownloadState
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.IntervalOverrides
import app.atemkraft.data.MeditationDisplay
import app.atemkraft.data.SessionDisplay
import app.atemkraft.data.Situations
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePhase
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.RuntimePhase
import app.atemkraft.domain.SessionKind
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.domain.SessionProgress
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import app.atemkraft.domain.buildTimeline
import app.atemkraft.domain.estimatedTotalSeconds
import app.atemkraft.domain.sessionSummary
import app.atemkraft.domain.withoutMarks
import app.atemkraft.playback.MeditationStatus
import app.atemkraft.playback.MeditationUiState
import app.atemkraft.playback.SessionStatus
import app.atemkraft.playback.SessionUiState
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.TestMatrix.Device
import app.atemkraft.ui.about.AboutScreen
import app.atemkraft.ui.atemlehre.AtemlehreScreen
import app.atemkraft.ui.components.MiniNowPlayingBar
import app.atemkraft.ui.detail.ExerciseDetailScreen
import app.atemkraft.ui.detail.SafetyDialog
import app.atemkraft.ui.glossary.GlossaryScreen
import app.atemkraft.ui.home.HomeScreen
import app.atemkraft.ui.learn.LearnScreen
import app.atemkraft.ui.learn.PranayamaScreen
import app.atemkraft.ui.log.LogbookScreen
import app.atemkraft.ui.meditation.MeditationOverlay
import app.atemkraft.ui.own.OwnPatternsScreen
import app.atemkraft.ui.patterns.PatternEditorState
import app.atemkraft.ui.session.SessionOverlay
import app.atemkraft.ui.settings.NoOpSettingsActions
import app.atemkraft.ui.settings.PatternTransfer
import app.atemkraft.ui.settings.SettingsScreen
import app.atemkraft.ui.settings.SettingsUiState
import app.atemkraft.ui.situations.SituationsScreen
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneId

/**
 * Alle Screens durch die Gates des [ScreenshotHarness] (Wortbruch, Abschneiden, Tippflächen) auf
 * der Breiten-Matrix ([TestMatrix.WIDTH_CONFIGS]): 360 dp bei Schrift 1,0 und 2,0, der
 * ungünstigste Fall für Wortbruch und Abschneiden. Seltene und teure Bilder (Detailseiten,
 * Sicherheitsdialog, Vorlagen-Sheet, Querformat) nur bei 2,0 ([worstCase]). Die höhenabhängigen
 * Gates des Muster-Editors laufen in [DeviceFitTest].
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Fenster groß genug für den höchsten Screen (fontScale 2,0); gerendert wird nur der Testknoten.
@Config(qualifiers = "w412dp-h8000dp-xhdpi")
class ScreenshotTest(device: Device, fontScale: Float) : ScreenshotHarness(device, fontScale) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_fs{1}")
        fun params(): List<Array<Any>> = TestMatrix.WIDTH_CONFIGS.map { (d, fs) -> arrayOf<Any>(d, fs) }

        /** Laufende Kurven-Vorschau: 4,5 s nach Play, mitten in „Halten voll“ von 4·1·6. */
        private const val PREVIEW_AT_MS = 4_500L
    }

    private val worstCase get() = fontScale == TestMatrix.MAX_FONT_SCALE

    private val app get() = RuntimeEnvironment.getApplication()

    /**
     * Die vier Tabs: Atmen; Situationen mit der größten aufgeklappt und mit Suchtreffern; „Eigene“
     * mit vier Mustern (eines mit langem Namen); Meditation mit offener Feinwahl auf 12 min (scrollt
     * bis 1,3 nicht, LAYOUT-08) und ∞ mit aufgeklapptem Klang (akzeptierte Scroll-Ausnahme S-40).
     */
    @Test
    fun tabs() {
        snap("01_atmen") {
            HomeScreen(
                exercisesByFamily = BreathingFamily.entries
                    .map { f -> f to BuiltInExercises.all.filter { it.family == f && it.guided } }
                    .filter { (_, list) -> list.isNotEmpty() },
                programs = BuiltInExercises.all.filter { !it.guided },
                daily = daily,
                dailySaved = false,
                onRegenerateDaily = {},
                onDailySavedChange = {},
                onSelect = {},
                onOpenSettings = {},
                onOpenPatternEditor = {},
                onOpenLogbook = {},
            )
        }
        val widest = Situations.all.maxBy { it.exerciseIds.size }
        snap("02_situationen_aufgeklappt", prepare = { compose.onNodeWithText(widest.title).performClick() }) { SituationsContent() }
        snap("02_situationen_suche_treffer", prepare = {
            compose.onNodeWithContentDescription(app.getString(R.string.cd_search_open)).performClick()
            compose.onNode(hasSetTextAction()).performTextInput("Angst vor der Prüfung")
        }) { SituationsContent() }
        snap("13_eigene", tab = Tab.EIGENE) {
            // „Tut mir gut“ mit den zwei längsten Übungsnamen.
            val favorites = BuiltInExercises.all.sortedByDescending { it.name.length }.take(2)
            OwnPatternsScreen(savedPatterns = saved, onNewPattern = {}, favorites = favorites, onSelect = {}, onDeletePattern = {}, onOpenSettings = {})
        }
        snap("03_meditation_fein", tab = Tab.MEDITATION, noScroll = true, prepare = {
            // Die Mitte ist klickbar und meldet ihren Zustand als „Feinwahl aus“.
            val closed = app.getString(R.string.meditation_dial_fine_off)
            compose.onNode(
                SemanticsMatcher("Mitte des Zeitrings") { node ->
                    node.config.getOrNull(SemanticsActions.OnClick) != null &&
                        node.config.getOrNull(SemanticsProperties.StateDescription) == closed
                },
            ).performClick()
        }) { MeditationTab(MeditationConfig(minutes = 12)) }
        snap("03_meditation_frei", tab = Tab.MEDITATION, acceptedScroll = true, prepare = {
            compose.onNodeWithText(app.getString(R.string.meditation_sound_voice_label)).performClick()
        }) { MeditationTab(MeditationConfig(mode = MeditationMode.FREE, gongEveryMin = 15), gongIntervalMin = 15) }
    }

    @Composable
    private fun SituationsContent() {
        SituationsScreen(
            recommendations = Situations.all,
            resolve = { id -> BuiltInExercises.all.firstOrNull { it.id == id } },
            onSelect = {},
        )
    }

    /**
     * Weitergeführte Seiten: Logbuch mit allen Gruppen; Einstellungen mit allen längsten Varianten
     * (Hinweis nach zurückgesetzten Einstellungen, „Keine Stimme gewählt“, Stimmen-Katalog offen
     * samt Quelle, „Beim Handywechsel?“ offen, längste Daten-Rückmeldungen, leeres Logbuch);
     * Glossar; Über mit Lizenzen; Atemlehre mit aufgeklappten Köpfen (samt den fünf vāyus) und der Karte „Feuer“;
     * Tab „Lernen“ in Gerätehöhe und Pranayama mit allen Karten aufgeklappt;
     * Mini-Leiste mit längstem Namen und längstem Phasenwort.
     */
    @Test
    fun pages() {
        snap("04_logbuch") { Logbook() }
        snap("05_einstellungen_daten", prepare = {
            compose.onNodeWithText(app.getString(R.string.settings_voice_show)).performClick()
            compose.onNodeWithText(app.getString(R.string.settings_data_transfer_more)).performClick()
        }) { Settings() }
        snap("06_glossar") { GlossaryScreen(onBack = {}) }
        snap("07_ueber_lizenzen") { AboutScreen(onBack = {}, licensesExpanded = true) }
        snap("12_atemlehre_offen", prepare = {
            listOf(R.string.atemlehre_vayus, R.string.atemlehre_rules, R.string.atemlehre_influences, R.string.atemlehre_sources).forEach {
                compose.onNodeWithText(app.getString(it).withoutMarks()).performClick()
            }
        }) { AtemlehreScreen(onBack = {}, onOpenExercise = {}, focus = "feuer") }
        snap("15_lernen", tab = Tab.LERNEN) { LearnScreen(onOpenAtemlehre = {}, onOpenPranayama = {}, onOpenGlossary = {}) }
        snap("16_pranayama_offen") { PranayamaScreen(onBack = {}, onOpenExercise = {}, expanded = true) }
        snap("11_mini_leiste", heightDp = 160) {
            val longestPhase = listOf(R.string.phase_inhale, R.string.phase_exhale, R.string.phase_hold_full, R.string.session_paused)
                .map(app::getString)
                .maxBy { it.length }
            MiniNowPlayingBar(title = BuiltInExercises.all.maxBy { it.name.length }.name, statusText = longestPhase, onClick = {})
        }
    }

    /** Relativ zum festen DAY, damit jede Gruppe vorkommt (Heute, Gestern, Diese Woche, zwei Monate). */
    @Composable
    private fun Logbook() {
        fun at(daysAgo: Long, hour: Int, minute: Int): Long = DAY.minusDays(daysAgo).atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val daysAgo = listOf(0L, 1L, 3L, 5L, 12L, 40L)
        // Runden und Dauer wie vom BreathSessionController geloggt.
        val entries = BuiltInExercises.all.take(6).mapIndexed { i, e ->
            SessionLogEntry(
                id = i + 1L,
                exerciseId = e.id,
                exerciseName = e.name,
                family = e.family,
                startedAtEpochMs = at(daysAgo[i], 7, 30),
                durationMs = if (e.rounds > 1) e.estimatedTotalSeconds() * 1_000L else (5 + i) * 60_000L + 5_000L,
                roundsCompleted = e.rounds,
            )
        } + SessionLogEntry(
            id = 99,
            exerciseId = "meditation",
            exerciseName = "Meditation",
            family = null,
            startedAtEpochMs = at(1, 21, 15),
            durationMs = 20 * 60_000L,
            roundsCompleted = 0,
            kind = SessionKind.MEDITATION,
        )
        LogbookScreen(entries = entries, onBack = {}, today = DAY)
    }

    @Composable
    private fun Settings() {
        SettingsScreen(
            state = SettingsUiState(
                soundMode = SoundMode.entries.first(),
                transition = TransitionEmphasis.entries.first(),
                volume = ToneVolume.entries.first(),
                haptics = true,
                showSafetyWarning = true,
                gongIntervalMin = 5,
                gongLong = false,
                voiceStates = VoiceCatalog.all.mapIndexed { i, v -> v.id to if (i == 0) VoiceDownloadState.Downloaded else VoiceDownloadState.NotDownloaded }.toMap(),
                activeVoiceId = null,
                piperEngineReady = true,
                settingsResetPending = true,
                logCount = 0,
            ),
            actions = NoOpSettingsActions,
            onOpenAbout = {},
            onBack = {},
            transfer = PatternTransfer(
                exportLabel = pluralStringResource(R.plurals.data_export_done, 120, 120),
                importLabel = pluralStringResource(R.plurals.data_import_done, 120, 120, 380),
                note = stringResource(R.string.data_import_not_backup),
            ),
        )
    }

    /**
     * Muster-Editor in Listenhöhe: Vorlage „Viloma I“ (sieben Phasen), zweite Phase gewählt,
     * „Ablauf“ und „Aus der Yoga-Lehre“ aufgeklappt; dann laufende Vorschau (Uhr fest, Punkt in
     * „Halten voll“) mit dem Phasennamen in der Startzeile.
     */
    @Test
    fun patternEditor() {
        snap("14_muster_editor_auswahl", prepare = {
            compose.onNodeWithText(app.getString(R.string.pattern_editor_sequence)).performClick()
            compose.onNodeWithText(app.getString(R.string.detail_lore)).performClick()
        }) {
            Editor { PatternEditorState.new(it, template("Viloma I")).apply { select(1) } }
        }
        snap("14_muster_editor_vorschau", prepare = {
            // Uhr erst anhalten, dann den Klick direkt auslösen: Die Vorschau läuft endlos.
            compose.waitForIdle()
            compose.mainClock.autoAdvance = false
            compose.onNodeWithContentDescription(app.getString(R.string.cd_preview_play)).performSemanticsAction(SemanticsActions.OnClick)
            compose.waitForIdle()
            compose.mainClock.advanceTimeBy(PREVIEW_AT_MS)
        }) {
            Editor { PatternEditorState.new(it, FreePattern(listOf(FreePhase(FreePhaseType.IN, 4), FreePhase(FreePhaseType.HOLD, 1), FreePhase(FreePhaseType.OUT, 6)))) }
        }
    }

    /** Startpunkt-Sheet offen: eigenes Fenster, also Fenster in Gerätegröße ([setUp]) und Aufnahme des ganzen Bildschirms. */
    @Test
    fun patternEditorTemplates() {
        assumeTrue(worstCase)
        snap("14_muster_editor_startpunkt_offen", tab = Tab.EIGENE, tabBar = false, screen = true, prepare = {
            compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_editor_start, "Box")).performClick()
            compose.waitForIdle()
        }) {
            Editor { PatternEditorState.new(it, template("Box")) }
        }
    }

    /**
     * Jede eingebaute Übung, das Muster des Tages und zwei eigene Muster (langer Name; intensiv mit
     * Stufen): „Aus der Yoga-Lehre“ aufgeklappt, wo es ihn gibt; 4-7-8 mit gespeicherter Anpassung
     * und aufgeklappten Phasen.
     */
    @Test
    fun details() {
        assumeTrue(worstCase)
        val intense = template("Viloma I").let { p -> FreePattern(p.phases.mapIndexed { i, phase -> if (i == 5) phase.copy(seconds = 12) else phase }) }
        val pages = (BuiltInExercises.all + daily.exercise).map { it.id to it } + listOf(
            "eigenes_muster" to saved.last().exercise,
            "eigenes_muster_intensiv" to RandomPatternGenerator.exerciseFrom(intense, "muster-5", "Stufen mit langem Halten", minutes = 5),
        )
        pages.forEach { (name, exercise) ->
            val builtIn = exercise in BuiltInExercises.all
            val adjusted = exercise.id == "4-7-8"
            snap("08_detail_$name", prepare = {
                if (exercise.lore != null) compose.onNodeWithText(app.getString(R.string.detail_lore)).performClick()
                if (adjusted) compose.onNodeWithText(app.getString(R.string.adjust_intervals)).performClick()
            }) {
                ExerciseDetailScreen(
                    exercise = exercise,
                    requireSafetyConfirm = false,
                    savedIntervals = if (adjusted) IntervalOverrides(duration = 6, inhale = 5, hold = 8, exhale = 9) else null,
                    onIntervalsChange = { _, _, _, _ -> },
                    onIntervalsReset = {},
                    onConfirmedSafety = {},
                    onStart = {},
                    onBack = {},
                    onOpenLore = {},
                    onMarkedChange = if (builtIn) ({ _: Boolean -> }) else null,
                    onEditPattern = if (builtIn) null else ({}),
                )
            }
        }
    }

    /**
     * Sicherheitsdialog (MUSTER-06) für die Übungen mit `caution`: eigenes Fenster in Gerätegröße
     * ([setUp]), Aufnahme des ganzen Bildschirms; bei großer Schrift scrollt die Hinweisliste,
     * statt still abzuschneiden.
     */
    @Test
    fun safetyDialog() {
        assumeTrue(worstCase)
        BuiltInExercises.all.filter { it.caution }.forEach { exercise ->
            snap("08_sicherheitsdialog_${exercise.id}", heightDp = windowDp.second, widthDp = windowDp.first, screen = true) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                SafetyDialog(cautions = exercise.cautions, onConfirm = {}, onDismiss = {})
            }
        }
    }

    /**
     * Vollbild-Session in Gerätehöhe: Vorbereitung, Einatmen, Viloma-Pause, offenes Halten (Wim Hof),
     * Pause, Ende mit Bilanz; dann die Meditation in allen Zuständen. Die Session-Zustände kommen
     * aus den Daten der Übung ([sessionAt]), so wie der BreathSessionController sie setzt.
     */
    @Test
    fun session() {
        val exercise = BuiltInExercises.all.first { it.id == "4-7-8" }
        val inhale = sessionAt(exercise, elapsedMs = 1_600) { it.roundIndex == 2 && it.type == PhaseType.INHALE }
        val states = mapOf(
            "vorbereitung" to SessionUiState(
                exerciseName = exercise.name,
                status = SessionStatus.PREPARING,
                roundCount = exercise.rounds,
                countdown = 3,
                patternHint = exercise.instructionHint,
            ),
            "einatmen" to inhale,
            "viloma_pause" to sessionAt(BuiltInExercises.all.first { it.id == "viloma-ein" }, elapsedMs = 800) { it.type == PhaseType.HOLD_FULL },
            "warten" to sessionAt(BuiltInExercises.all.first { it.id == "wim-hof" }, elapsedMs = 23_000) { it.roundIndex == 1 && it.duration !is PhaseDuration.Fixed },
            "pause" to inhale.copy(status = SessionStatus.PAUSED),
            "ende" to inhale.copy(
                status = SessionStatus.FINISHED,
                phaseType = null,
                remainingMs = 0L,
                progress = 1f,
                summary = sessionSummary(exercise.buildTimeline(), exercise.estimatedTotalSeconds() * 1_000L),
            ),
        )
        states.forEach { (name, state) -> snap("09_session_$name", heightDp = device.heightDp) { SessionShot(state) } }
        val meditation = mapOf(
            "vorbereitung" to MeditationUiState(status = MeditationStatus.PREPARING, countdown = 3),
            "laeuft" to MeditationUiState(status = MeditationStatus.RUNNING, remainingMs = 7 * 60_000L + 12_000L, elapsedMs = 2 * 60_000L + 48_000L),
            "pause" to MeditationUiState(status = MeditationStatus.PAUSED, remainingMs = 7 * 60_000L + 12_000L, elapsedMs = 2 * 60_000L + 48_000L),
            "ende" to MeditationUiState(status = MeditationStatus.FINISHED, elapsedMs = 10 * 60_000L),
        )
        meditation.forEach { (name, state) -> snap("10_meditation_$name", heightDp = device.heightDp) { MeditationShot(state) } }
    }

    /** Querformat: Steuerung rechts neben dem Kreis, Breite und Höhe getauscht (Statusleiste vernachlässigt). */
    @Test
    @Config(qualifiers = "w920dp-h8000dp-xhdpi")
    fun landscape() {
        assumeTrue(worstCase)
        val box = BuiltInExercises.all.first { it.id == "box-4-4-4-4" }
        val state = sessionAt(box, elapsedMs = 1_600) { it.roundIndex == 2 && it.type == PhaseType.INHALE }
        snap("09_session_quer", heightDp = device.widthDp, widthDp = device.heightDp) { SessionShot(state) }
        snap("10_meditation_quer", heightDp = device.widthDp, widthDp = device.heightDp) {
            MeditationShot(MeditationUiState(status = MeditationStatus.RUNNING, remainingMs = 7 * 60_000L + 12_000L, elapsedMs = 2 * 60_000L + 48_000L))
        }
    }

    /**
     * Zustand der laufenden Session wie im BreathSessionController (runFixedPhase/runUserPacedPhase): die
     * erste Phase des Ablaufs von [exercise], auf die [phase] zutrifft, davon [elapsedMs] verstrichen
     * (bei offenen Phasen hochgezählt).
     */
    private fun sessionAt(exercise: Exercise, elapsedMs: Long, phase: (RuntimePhase) -> Boolean): SessionUiState {
        val timeline = exercise.buildTimeline()
        val index = timeline.indexOfFirst(phase)
        val current = timeline[index]
        val totalMs = (current.duration as? PhaseDuration.Fixed)?.millis
        return SessionUiState(
            exerciseName = exercise.name,
            status = if (totalMs != null) SessionStatus.RUNNING else SessionStatus.WAITING_FOR_USER,
            phaseType = current.type,
            phaseLevel = current.level,
            phaseLabel = current.label,
            phaseNote = current.note,
            remainingMs = totalMs?.let { it - elapsedMs } ?: 0L,
            phaseTotalMs = totalMs ?: 0L,
            elapsedMs = if (totalMs != null) 0L else elapsedMs,
            roundIndex = current.roundIndex,
            roundCount = current.roundCount,
            nextPhaseType = timeline.getOrNull(index + 1)?.type,
            patternHint = exercise.instructionHint,
            progress = SessionProgress(timeline).at(index, if (totalMs != null) elapsedMs else 0L),
        )
    }

    /** Ohne automatische Ruhe, damit nichts von selbst ausblendet. */
    @Composable
    private fun SessionShot(state: SessionUiState) = SessionOverlay(
        state = state,
        display = SessionDisplay(autoCalm = false),
        onMinimize = {},
        onToggleMute = {},
        onTogglePause = {},
        onTapHaptic = {},
        onContinue = {},
        onRestart = {},
        onStop = {},
        onOpenLogbook = {},
    )

    @Composable
    private fun MeditationShot(state: MeditationUiState) = MeditationOverlay(
        state = state,
        display = MeditationDisplay(autoCalm = false),
        onMinimize = {},
        onToggleMute = {},
        onTogglePause = {},
        onTapHaptic = {},
        onRestart = {},
        onEnd = {},
        onOpenLogbook = {},
    )
}
