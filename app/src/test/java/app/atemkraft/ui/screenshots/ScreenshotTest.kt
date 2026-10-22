package app.atemkraft.ui.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
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
import app.atemkraft.playback.MeditationStatus
import app.atemkraft.playback.MeditationUiState
import app.atemkraft.playback.SessionStatus
import app.atemkraft.ui.TestDetail
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.TestMatrix.Device
import app.atemkraft.ui.TestMeditationOverlay
import app.atemkraft.ui.TestSessionOverlay
import app.atemkraft.ui.about.AboutScreen
import app.atemkraft.ui.atemlehre.AtemlehreScreen
import app.atemkraft.ui.components.MiniNowPlayingBar
import app.atemkraft.ui.detail.SafetyDialog
import app.atemkraft.ui.glossary.GlossaryScreen
import app.atemkraft.ui.home.HomeScreen
import app.atemkraft.ui.log.LogbookScreen
import app.atemkraft.ui.own.OwnPatternsScreen
import app.atemkraft.ui.patterns.PatternEditorState
import app.atemkraft.ui.session.SessionUiState
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
 * Alle Screens auf der Breiten-Matrix ([TestMatrix.WIDTH_CONFIGS]): 360 dp bei 1,0, 1,3 und 2,0
 * ist für Wortbruch, Abschneiden, Tippflächen und Scrollen der ungünstigste Fall (schmalste
 * Breite, kleinste Höhe), dazu das A54 bei 1,0 als Bild des Hauptgeräts und Quelle der
 * README-Bilder. Die höhenabhängigen Gates (Muster-Editor, Meditations-Tab) laufen in
 * [DeviceFitTest] über alle Geräte. Seltene und teure Bilder nur in [TestMatrix.isKeyConfig],
 * Detailseiten in [TestMatrix.isDetailConfig].
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

    private val keyConfig get() = TestMatrix.isKeyConfig(device, fontScale)

    // Detailseiten (eine pro Übung) nur in den aussagekräftigsten Kombinationen, sonst dauert
    // der Lauf zu lange.
    private val detailConfig get() = TestMatrix.isDetailConfig(device, fontScale)

    private val app get() = RuntimeEnvironment.getApplication()

    @Test
    fun home() = snap("01_atmen") {
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

    /**
     * Vorlage „Viloma I“ (sieben Phasen, Stufen, „Halten“ statt „Halten voll“), zweite Phase
     * gewählt: Werkzeugleiste mit Papierkorb, „Ablauf“ und „Aus der Yoga-Lehre“ aufgeklappt.
     */
    @Test
    fun patternEditorSelection() = snap("14_muster_editor_auswahl", prepare = {
        compose.onNodeWithText(app.getString(R.string.pattern_editor_sequence)).performClick()
        compose.onNodeWithText(app.getString(R.string.detail_lore)).performClick()
    }) {
        Editor { PatternEditorState.new(it, template("Viloma I")).apply { select(1) } }
    }

    /**
     * Startpunkt-Sheet offen (Nachtrag 6): die sechs Vorlagen mit Mini-Kurve, „Box“ markiert. Das
     * Sheet ist ein eigenes Fenster: Fenster in Gerätegröße ([setUp]), Aufnahme des ganzen Bildschirms.
     */
    @Test
    fun patternEditorTemplates() {
        assumeTrue(keyConfig)
        snap("14_muster_editor_startpunkt_offen", tab = Tab.EIGENE, screen = true, prepare = {
            compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_editor_start, "Box")).performClick()
            compose.waitForIdle()
        }) {
            Editor { PatternEditorState.new(it, template("Box")) }
        }
    }

    /**
     * Vorschau der Kurve läuft (Uhr fest, Punkt in „Halten voll“): Phasenname in Phasenfarbe am
     * Kopf „Atemzug“ statt des Status, bricht bei großer Schrift nur zwischen Wörtern um (LAYOUT-03).
     */
    @Test
    fun patternEditorPreview() = snap("14_muster_editor_vorschau", prepare = {
        // Uhr erst anhalten, dann den Klick direkt auslösen: Die Vorschau läuft endlos.
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription(app.getString(R.string.cd_preview_play))
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        // 4·1·6: Halten voll von 4 bis 5 s – der längste Phasenname.
        compose.mainClock.advanceTimeBy(PREVIEW_AT_MS)
    }) {
        Editor { PatternEditorState.new(it, FreePattern(listOf(FreePhase(FreePhaseType.IN, 4), FreePhase(FreePhaseType.HOLD, 1), FreePhase(FreePhaseType.OUT, 6)))) }
    }

    /** Tab „Eigene“ mit vier Mustern (das letzte im Editor zusammengestellt, langer Name): „Neues Muster“, darunter „Meine Muster“ immer offen. */
    @Test
    fun ownPatterns() = snap("13_eigene", tab = Tab.EIGENE) { OwnPatternsContent() }

    /** Tab „Eigene“ ohne Muster: kurzer Satz und „Neues Muster“. */
    @Test
    fun ownPatternsEmpty() {
        assumeTrue(keyConfig)
        snap("13_eigene_leer", tab = Tab.EIGENE) { OwnPatternsContent(patterns = 0) }
    }

    @Composable
    private fun OwnPatternsContent(patterns: Int = saved.size) {
        OwnPatternsScreen(
            savedPatterns = saved.take(patterns),
            onNewPattern = {},
            onSelect = {},
            onDeletePattern = {},
            onOpenSettings = {},
        )
    }

    /** Eine Situation aufgeklappt (die mit den meisten Übungen), alle übrigen mit Titel und Teaser zu. */
    @Test
    fun situationsExpanded() {
        val widest = Situations.all.maxBy { it.exerciseIds.size }
        snap("02_situationen_aufgeklappt", prepare = {
            compose.onNodeWithText(widest.title).performClick()
        }) { SituationsContent() }
    }

    /** Befindens-Suche offen, noch leer: Feld mit Platzhalter statt Untertitel, X rechts. */
    @Test
    fun situationsSearchEmpty() {
        assumeTrue(keyConfig)
        snap("02_situationen_suche", tab = Tab.SITUATIONEN, prepare = { openSearch() }) { SituationsContent() }
    }

    /** Suche mit zwei Treffern: aufgeklappt, beste zuerst, Trefferzahl darüber. */
    @Test
    fun situationsSearchHits() = snap("02_situationen_suche_treffer", prepare = {
        openSearch()
        compose.onNode(hasSetTextAction()).performTextInput("Angst vor der Prüfung")
    }) { SituationsContent() }

    /** Suche ohne Treffer: „Nichts gefunden“ mit „Alle zeigen“. */
    @Test
    fun situationsSearchNone() {
        assumeTrue(keyConfig)
        snap("02_situationen_suche_leer", tab = Tab.SITUATIONEN, prepare = {
            openSearch()
            compose.onNode(hasSetTextAction()).performTextInput("Asthma")
        }) { SituationsContent() }
    }

    private fun openSearch() {
        compose.onNodeWithContentDescription(app.getString(R.string.cd_search_open)).performClick()
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
     * Feinwahl offen auf 12 min: − und + neben der Zahl, Cyan-Label am Knopf (zwischen zwei Zahlen),
     * Reset im Split-Button (gleiche Höhe wie Start). Scrollt bis 1,3 nicht (LAYOUT-08).
     */
    @Test
    fun meditationFine() = snap("03_meditation_fein", tab = Tab.MEDITATION, noScroll = true, prepare = {
        // Die Mitte ist klickbar und meldet ihren Zustand als „Feinwahl aus“.
        val closed = app.getString(R.string.meditation_dial_fine_off)
        compose.onNode(
            SemanticsMatcher("Mitte des Zeitrings") { node ->
                node.config.getOrNull(SemanticsActions.OnClick) != null &&
                    node.config.getOrNull(SemanticsProperties.StateDescription) == closed
            },
        ).performClick()
    }) {
        MeditationTab(MeditationConfig(minutes = 12))
    }

    /**
     * Modus „Frei“ (∞): Bogen voll in Pink; „Klang & Stimme“ aufgeklappt mit Gong „alle N min“
     * (längstes Segment) – Ring am kleinsten. Aufgeklappt scrollt die Seite auf 360 dp schon bei
     * fontScale 1,0 – akzeptierte Ausnahme von LAYOUT-08 (S-40, Eigentümer 2026-10-19: der
     * Abschnitt ist standardmäßig zu); nur gemessen (`SCROLLT_AUSNAHME`), nicht als Fehler gewertet.
     */
    @Test
    fun meditationFree() = snap("03_meditation_frei", tab = Tab.MEDITATION, acceptedScroll = true, prepare = {
        compose.onNodeWithText(app.getString(R.string.meditation_sound_voice_label)).performClick()
    }) {
        MeditationTab(MeditationConfig(mode = MeditationMode.FREE, gongEveryMin = 15), gongIntervalMin = 15)
    }

    @Test
    fun logbook() = snap("04_logbuch") {
        // Relativ zum festen DAY, damit jede Gruppe vorkommt: Heute, Gestern, Diese Woche, zwei Monate;
        // ein Tag mit zwei Sitzungen.
        fun at(daysAgo: Long, hour: Int, minute: Int): Long = DAY.minusDays(daysAgo).atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val daysAgo = listOf(0L, 1L, 3L, 5L, 12L, 40L)
        // Runden und Dauer wie vom SessionViewModel geloggt: Runden = die der Übung, rundenbasierte
        // dauern ihren Ablauf, die übrigen die gewählten Minuten.
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
        ) + SessionLogEntry(
            // Zweite Sitzung am Tag vor 12 Tagen: zeigt die Bündelung je Tag in einer Monatsgruppe.
            id = 98,
            exerciseId = "meditation",
            exerciseName = "Meditation",
            family = null,
            startedAtEpochMs = at(12, 20, 45),
            durationMs = 15 * 60_000L,
            roundsCompleted = 0,
            kind = SessionKind.MEDITATION,
        )
        LogbookScreen(entries = entries, onBack = {}, today = DAY)
    }

    @Test
    fun logbookEmpty() {
        assumeTrue(keyConfig)
        snap("04_logbuch_leer", tab = Tab.ATMEN) {
            LogbookScreen(entries = emptyList(), onBack = {}, today = DAY)
        }
    }

    /** Standardbild: Stimmen-Katalog offen (eine Stimme lädt), Logbuch mit 12 Einträgen. */
    @Test
    fun settings() = snap("05_einstellungen") { Settings() }

    /**
     * Alle längsten Varianten in einem Bild: einmaliger Hinweis nach zurückgesetzten Einstellungen,
     * „Keine Stimme gewählt“, Stimmen-Katalog per „Stimmen zeigen“ aufgeklappt (Hinweis mit Quelle,
     * SEC-NET-02), „Beim Handywechsel?“ offen, längste Daten-Rückmeldungen und leeres Logbuch
     * („Noch keine Sitzung“, ohne Papierkorb) – LAYOUT-03.
     */
    @Test
    fun settingsDataStatus() = snap("05_einstellungen_daten", prepare = {
        compose.onNodeWithText(app.getString(R.string.settings_voice_show)).performClick()
        compose.onNodeWithText(app.getString(R.string.settings_data_transfer_more)).performClick()
    }) {
        Settings(
            downloading = false,
            voiceActive = false,
            resetPending = true,
            logCount = 0,
            transfer = PatternTransfer(
                exportLabel = pluralStringResource(R.plurals.data_export_done, 120, 120),
                importLabel = pluralStringResource(R.plurals.data_import_done, 120, 120, 380),
                note = stringResource(R.string.data_import_not_backup),
            ),
        )
    }

    /**
     * Einstellungen mit einer geladenen Stimme (die erste); [downloading] lädt die zweite (der
     * Katalog bleibt dann offen), [voiceActive] = die geladene ist gewählt; [logCount] Einträge
     * im Logbuch.
     */
    @Composable
    private fun Settings(
        transfer: PatternTransfer = PatternTransfer.Idle,
        downloading: Boolean = true,
        voiceActive: Boolean = true,
        resetPending: Boolean = false,
        logCount: Int = 12,
    ) {
        val voices = VoiceCatalog.all.mapIndexed { i, v ->
            v.id to when {
                i == 0 -> VoiceDownloadState.Downloaded
                i == 1 && downloading -> VoiceDownloadState.Downloading(fraction = 0.4f)
                else -> VoiceDownloadState.NotDownloaded
            }
        }.toMap()
        SettingsScreen(
            state = SettingsUiState(
                soundMode = SoundMode.entries.first(),
                transition = TransitionEmphasis.entries.first(),
                volume = ToneVolume.entries.first(),
                haptics = true,
                showSafetyWarning = true,
                gongIntervalMin = 5,
                gongLong = false,
                voiceStates = voices,
                activeVoiceId = if (voiceActive) VoiceCatalog.all.first().id else null,
                piperEngineReady = true,
                settingsResetPending = resetPending,
                logCount = logCount,
            ),
            actions = NoOpSettingsActions,
            onOpenGlossary = {},
            onOpenAtemlehre = {},
            onOpenAbout = {},
            onBack = {},
            transfer = transfer,
        )
    }

    @Test
    fun glossary() = snap("06_glossar") { GlossaryScreen(onBack = {}) }

    /** Über-Screen mit aufgeklappten Lizenzen: Bausteine, Stimmen, Links mit 48-dp-Zeilen. */
    @Test
    fun aboutLicenses() = snap("07_ueber_lizenzen") { AboutScreen(onBack = {}, licensesExpanded = true) }

    /**
     * Jede eingebaute Übung plus zwei Muster des Tages (eines mit beiden Halten): Name im festen
     * Kopf (neben der Markierung wie in der App, beim Muster ohne), darunter Familie und Chips;
     * „Aus der Yoga-Lehre“ aufgeklappt, wo es ihn gibt. 4-7-8 mit gespeicherter Anpassung: Reset
     * sichtbar, Phasen aufgeklappt (Stepper in der Karte, Split-Button).
     */
    @Test
    fun details() {
        assumeTrue(detailConfig)
        val dailyWithHolds = (0L until 200L).map(RandomPatternGenerator::forSeed).first { it.spec.holdEmpty != null }.exercise
        (BuiltInExercises.all + daily.exercise + dailyWithHolds).forEach { exercise ->
            val builtIn = exercise in BuiltInExercises.all
            val adjusted = exercise.id == "4-7-8"
            val name = if (builtIn || exercise == daily.exercise) exercise.id else "${exercise.id}_halten"
            snap("08_detail_$name", prepare = {
                if (exercise.lore != null) compose.onNodeWithText(app.getString(R.string.detail_lore)).performClick()
                if (adjusted) compose.onNodeWithText(app.getString(R.string.adjust_intervals)).performClick()
            }) {
                TestDetail(
                    exercise = exercise,
                    savedIntervals = if (adjusted) IntervalOverrides(duration = 6, inhale = 5, hold = 8, exhale = 9) else null,
                    onOpenLore = {},
                    onMarkedChange = if (builtIn) ({ _: Boolean -> }) else null,
                    // Stift im Kopf des Tagesmusters (öffnet den Muster-Editor).
                    onEditPattern = if (builtIn) null else ({}),
                )
            }
        }
    }

    /**
     * Detailseiten eigener Muster mit Stift im Kopf: frei gewählter langer Name, die Folge im Text
     * und nur die Dauer in der Karte; dazu ein intensives mit Stufen (Viloma I, letztes Halten
     * 12 s): Chip „intensiv“, Sicherheitshinweise offen.
     */
    @Test
    fun detailsCustom() {
        assumeTrue(detailConfig)
        val intense = template("Viloma I").let { p -> FreePattern(p.phases.mapIndexed { i, phase -> if (i == 5) phase.copy(seconds = 12) else phase }) }
        listOf(
            "08_detail_eigenes_muster" to saved.last().exercise,
            "08_detail_eigenes_muster_intensiv" to RandomPatternGenerator.exerciseFrom(intense, "muster-5", "Stufen mit langem Halten", minutes = 5),
        ).forEach { (name, exercise) ->
            snap(name) { TestDetail(exercise = exercise, onOpenLore = {}, onEditPattern = {}) }
        }
    }

    /**
     * Sicherheitsdialog (MUSTER-06) für beide Übungen mit `caution`. Der Dialog ist ein eigenes
     * Fenster und misst sich daran: Fenster in Gerätegröße ([setUp]), Aufnahme des ganzen
     * Bildschirms; Text- und Bedienprüfung laufen über alle Roots, also auch über das
     * Dialogfenster. Bei großer Schrift scrollt die Hinweisliste, statt still abzuschneiden.
     */
    @Test
    fun safetyDialog() {
        assumeTrue(detailConfig)
        safetyDialogs("")
    }

    /** Dasselbe quer: nur im ungünstigsten Fall (360 dp bei Schrift 2,0, also 640 × 360). */
    @Test
    fun safetyDialogQuer() {
        assumeTrue(device == TestMatrix.COMPACT && fontScale == TestMatrix.MAX_FONT_SCALE)
        safetyDialogs("_quer")
    }

    private fun safetyDialogs(suffix: String) {
        BuiltInExercises.all.filter { it.caution }.forEach { exercise ->
            snap("08_sicherheitsdialog_${exercise.id}$suffix", heightDp = windowDp.second, widthDp = windowDp.first, screen = true) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                SafetyDialog(cautions = exercise.cautions, onConfirm = {}, onDismiss = {})
            }
        }
    }

    /**
     * Vollbild-Session in echter Gerätehöhe: Vorbereitung, Phase mit Anleitungszeile, offene Phase,
     * Pause, Ruhe-Ansicht (Tipp neben den Kreis, ohne Ring und Sekunden), Ende mit Bilanz (mit und
     * ohne Runden). Alle Zustände kommen aus den Daten der Übung ([sessionAt]), so wie das
     * SessionViewModel sie setzt – keine Kombination, die es nicht gibt.
     */
    @Test
    fun session() {
        val exercise = BuiltInExercises.all.first { it.id == "4-7-8" }
        val viloma = BuiltInExercises.all.first { it.id == "viloma-ein" }
        val wimHof = BuiltInExercises.all.first { it.id == "wim-hof" }
        // Resonanz: eine Runde nach Minuten – die Bilanz steht ohne Runden da.
        val resonance = BuiltInExercises.all.first { it.id == "resonanz" }
        // 4-7-8 in Runde 3 von 4, Einatmen nach 1,6 s.
        val inhale = sessionAt(exercise, elapsedMs = 1_600) { it.roundIndex == 2 && it.type == PhaseType.INHALE }
        val states = mapOf(
            // Im Countdown steht der Bogen noch bei 0 (SessionViewModel.start).
            "vorbereitung" to SessionUiState(
                exerciseName = exercise.name,
                status = SessionStatus.PREPARING,
                roundCount = exercise.rounds,
                countdown = 3,
                patternHint = exercise.instructionHint,
            ),
            "einatmen" to inhale,
            // Viloma: Pause nach der ersten Einatemstufe – Kreis steht bei ⅓. Viloma hat keine
            // Runden, der Kopf zeigt also nur den Namen.
            "viloma_pause" to sessionAt(viloma, elapsedMs = 800) { it.type == PhaseType.HOLD_FULL },
            // Offener Hold gibt es nur bei Wim Hof (Halten auf leerer Lunge bis zum Atemreiz).
            "warten" to sessionAt(wimHof, elapsedMs = 23_000) { it.roundIndex == 1 && it.duration !is PhaseDuration.Fixed },
            "ende" to inhale.copy(
                status = SessionStatus.FINISHED,
                phaseType = null,
                remainingMs = 0L,
                progress = 1f,
                summary = sessionSummary(exercise.buildTimeline(), exercise.estimatedTotalSeconds() * 1_000L),
            ),
            // Pause: Kreis steht, der Hauptknopf heißt „Weiter“.
            "pause" to inhale.copy(status = SessionStatus.PAUSED),
            "ende_ohne_runden" to sessionAt(resonance, elapsedMs = 0) { it.type == PhaseType.INHALE }.copy(
                status = SessionStatus.FINISHED,
                phaseType = null,
                remainingMs = 0L,
                progress = 1f,
                summary = sessionSummary(resonance.buildTimeline(), 5 * 60_000L + 24_000L),
            ),
        )
        states.forEach { (name, state) ->
            snap("09_session_$name", heightDp = device.heightDp) { SessionShot(state) }
        }
        // Ganz ruhig: Tipp neben den Kreis (linker Rand, Bildmitte) blendet die Bedienelemente aus;
        // Ring und Sekunden abgeschaltet – nur Kreis und Wort.
        snap("09_session_ganz_ruhig", heightDp = device.heightDp, prepare = {
            compose.onNodeWithTag(SHOT).performTouchInput { click(Offset(left + 4.dp.toPx(), centerY)) }
        }) { SessionShot(inhale, SessionDisplay(showProgress = false, showSeconds = false, autoCalm = false)) }
    }

    /**
     * Zustand der laufenden Session wie im SessionViewModel (runFixedPhase/runUserPacedPhase): die
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

    /**
     * Querformat (nicht gesperrt): Steuerung rechts neben dem Kreis, Anleitungszeile und „Als
     * Nächstes“ ohne Überlappung. Nur Testgerät und kleinstes Gerät mit großer Schrift; Breite und
     * Höhe getauscht (Statusleiste vernachlässigt).
     */
    @Test
    @Config(qualifiers = "w920dp-h8000dp-xhdpi")
    fun sessionQuer() {
        assumeTrue(keyConfig)
        // Box-Atmung in Runde 3 von 8, Einatmen nach 1,6 s.
        val box = BuiltInExercises.all.first { it.id == "box-4-4-4-4" }
        val state = sessionAt(box, elapsedMs = 1_600) { it.roundIndex == 2 && it.type == PhaseType.INHALE }
        snap("09_session_quer", heightDp = device.widthDp, widthDp = device.heightDp) { SessionShot(state) }
        snap("10_meditation_quer", heightDp = device.widthDp, widthDp = device.heightDp) {
            TestMeditationOverlay(
                state = MeditationUiState(status = MeditationStatus.RUNNING, remainingMs = 7 * 60_000L + 12_000L, elapsedMs = 2 * 60_000L + 48_000L),
            )
        }
    }

    @Composable
    private fun SessionShot(state: SessionUiState, display: SessionDisplay = SessionDisplay(autoCalm = false)) = TestSessionOverlay(
        state = state,
        display = display,
    )

    /** Laufende Meditation in Gerätehöhe: Vorbereitung, Timer läuft, Pause, Ende, Ruhe-Ansicht ohne Ring und Zeit. */
    @Test
    fun meditationSession() {
        val states = mapOf(
            "vorbereitung" to MeditationUiState(status = MeditationStatus.PREPARING, countdown = 3),
            "laeuft" to MeditationUiState(status = MeditationStatus.RUNNING, remainingMs = 7 * 60_000L + 12_000L, elapsedMs = 2 * 60_000L + 48_000L),
            "pause" to MeditationUiState(status = MeditationStatus.PAUSED, remainingMs = 7 * 60_000L + 12_000L, elapsedMs = 2 * 60_000L + 48_000L),
            "ende" to MeditationUiState(status = MeditationStatus.FINISHED, elapsedMs = 10 * 60_000L),
        )
        states.forEach { (name, state) ->
            snap("10_meditation_$name", heightDp = device.heightDp) {
                TestMeditationOverlay(
                    state = state,
                )
            }
        }
        // Ganz ruhig: Tipp neben den Kreis, Ring und Zeit abgeschaltet – nur der stille Kreis.
        snap("10_meditation_ganz_ruhig", heightDp = device.heightDp, prepare = {
            compose.onNodeWithTag(SHOT).performTouchInput { click(Offset(left + 4.dp.toPx(), centerY)) }
        }) {
            TestMeditationOverlay(
                state = states.getValue("laeuft"),
                display = MeditationDisplay(autoCalm = false, showProgress = false, showTime = false),
            )
        }
    }

    /**
     * Atemlehre mit aufgeklappten Köpfen (Grundregeln, Weitere Einflüsse mit Chips, Quellen) und,
     * wie nach dem Sprung aus der Detailseite, der Karte „Feuer“ (Lehre, Studien, Vorsicht-Chip,
     * Warnung); die übrigen Karten zu mit Rhythmusbild und Teaser.
     */
    @Test
    fun atemlehreOffen() = snap("12_atemlehre_offen", prepare = {
        listOf(R.string.atemlehre_rules, R.string.atemlehre_influences, R.string.atemlehre_sources).forEach {
            compose.onNodeWithText(app.getString(it)).performClick()
        }
    }) { AtemlehreScreen(onBack = {}, onOpenExercise = {}, focus = "feuer") }

    /**
     * Mini-Leiste mit dem längsten Übungsnamen und dem längsten Phasenwort: Der Name bricht an
     * Wortgrenzen um, nichts wird gekürzt (LAYOUT-03, früher Ellipse).
     */
    @Test
    fun miniBar() = snap("11_mini_leiste", heightDp = 160) {
        val longestPhase = listOf(R.string.phase_inhale, R.string.phase_exhale, R.string.phase_hold_full, R.string.session_paused)
            .map(app::getString)
            .maxBy { it.length }
        MiniNowPlayingBar(title = BuiltInExercises.all.maxBy { it.name.length }.name, statusText = longestPhase, onClick = {})
    }
}
