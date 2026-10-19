package app.atemkraft.ui.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.cue.tts.VoiceCatalog
import app.atemkraft.cue.tts.VoiceDownloadState
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.IntervalOverrides
import app.atemkraft.data.SavedPattern
import app.atemkraft.data.Situations
import app.atemkraft.domain.BreathLevel
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.SessionKind
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import app.atemkraft.ui.about.AboutScreen
import app.atemkraft.ui.atemlehre.AtemlehreScreen
import app.atemkraft.ui.components.AppNavItem
import app.atemkraft.ui.components.AppNavigationBar
import app.atemkraft.ui.components.CHIP_TEXT_TAG
import app.atemkraft.ui.components.badBreakAt
import app.atemkraft.ui.detail.ExerciseDetailScreen
import app.atemkraft.ui.glossary.GlossaryScreen
import app.atemkraft.ui.home.HomeScreen
import app.atemkraft.ui.log.LogbookScreen
import app.atemkraft.ui.meditation.MeditationOverlay
import app.atemkraft.ui.meditation.MeditationScreen
import app.atemkraft.ui.meditation.MeditationStatus
import app.atemkraft.ui.meditation.MeditationUiState
import app.atemkraft.ui.session.SessionOverlay
import app.atemkraft.ui.session.SessionStatus
import app.atemkraft.ui.session.SessionUiState
import app.atemkraft.ui.settings.DataStatus
import app.atemkraft.ui.settings.SettingsScreen
import app.atemkraft.ui.situations.SituationsScreen
import app.atemkraft.ui.theme.AtemkraftTheme
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.roborazziSystemPropertyOutputDirectory
import kotlinx.coroutines.awaitCancellation
import org.junit.After
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * Screenshots aller Screens auf typischen Gerätebreiten und Schriftgrößen (A-12, LAYOUT-03).
 * Läuft auf der JVM (Robolectric), kein Gerät nötig. Listen-Screens werden mit großer Höhe
 * gerendert, damit der gesamte Scroll-Inhalt auf einem Bild steht; Vollbild-Sessions in
 * echter Gerätehöhe.
 *
 * Endlos-Animationen stehen still (FrozenInfiniteAnimations).
 *
 * Bilder erzeugen: `./gradlew recordRoborazziDebug` → `app/build/outputs/roborazzi/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Fenster groß genug für den höchsten Screen (fontScale 2,0); gerendert wird nur der Testknoten.
@Config(sdk = [35], qualifiers = "w412dp-h8000dp-xhdpi")
class ScreenshotTest(private val device: Device, private val fontScale: Float) {

    /** Breite × Höhe in dp, wie das Gerät sie der App meldet (Standard-Anzeigegröße). */
    data class Device(val name: String, val widthDp: Int, val heightDp: Int) {
        override fun toString() = name
    }

    companion object {
        private val DEVICES = listOf(
            // Kleine Geräte und große Anzeigegröße auf Mittelklasse-Geräten.
            Device("kompakt360", 360, 640),
            // Galaxy A54 (1080 × 2340 px, 450 dpi): das Testgerät des Maintainers.
            Device("a54", 384, 832),
            // Pixel 7/8, Galaxy S24+ und ähnliche.
            Device("gross412", 412, 915),
        )

        // 1,0 Standard · 1,1 Samsung-Voreinstellung des A54 · 1,3 groß · 2,0 maximal (LAYOUT-03).
        private val FONT_SCALES = listOf(1.0f, 1.1f, 1.3f, 2.0f)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_fs{1}")
        fun params(): List<Array<Any>> = DEVICES.flatMap { d -> FONT_SCALES.map { arrayOf<Any>(d, it) } }

        private const val SHOT = "screenshot"

        /** Höhe für Listen-Screens bei fontScale 1,0; wächst mit der Schrift. */
        private const val TALL_DP = 4000

        // Feste Werte statt „heute“, damit die Bilder reproduzierbar sind.
        private val DAY = LocalDate.of(2026, 10, 3)
        private val daily = RandomPatternGenerator.forDate(DAY)
        private val saved = listOf(11L, 42L, 7L).map(RandomPatternGenerator::forSeed)
            .mapIndexed { i, p -> SavedPattern(id = i + 1L, exercise = p.exercise, spec = p.spec) }
    }

    private val tallDp get() = (TALL_DP * fontScale).toInt()

    // Detailseiten (eine pro Übung) nur in den aussagekräftigsten Kombinationen, sonst dauert
    // der Lauf zu lange: Testgerät mit Voreinstellung, kleinstes Gerät mit großer Schrift.
    private val detailConfig get() =
        (device.name == "a54" && fontScale == 1.1f) || (device.name == "kompakt360" && fontScale >= 1.3f)

    // Endlos-Animationen (Schimmer des Atemkreises) einfrieren: Der Test wartet sonst bei jedem
    // Bild bis zum Timeout auf Leerlauf (~150 s pro Session-Screenshot).
    @OptIn(ExperimentalTestApi::class)
    @get:Rule
    val compose = createComposeRule(effectContext = FrozenInfiniteAnimations)

    private object FrozenInfiniteAnimations : InfiniteAnimationPolicy {
        override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R = awaitCancellation()
    }

    // Ein setContent pro Test; snap() tauscht nur den Inhalt aus.
    private var current by mutableStateOf<(@Composable () -> Unit)?>(null)
    private var currentHeightDp by mutableIntStateOf(0)

    @Before
    fun setUp() {
        // Schriftgröße wie in den Android-Einstellungen, über die echte Konfiguration: Ab
        // Android 14 skaliert das System nichtlinear (große Schrift wächst weniger), das bildet
        // nur der FontScaleConverter der Plattform ab, nicht ein linearer Density-Override.
        RuntimeEnvironment.setFontScale(fontScale)
        compose.setContent {
            AtemkraftTheme {
                // Breite/Höhe wie auf dem Gerät.
                Box(
                    Modifier
                        .requiredSize(device.widthDp.dp, currentHeightDp.dp)
                        .testTag(SHOT),
                ) { current?.invoke() }
            }
        }
    }

    /** [prepare] läuft nach dem Setzen des Inhalts und vor der Aufnahme (z. B. etwas aufklappen). */
    private fun snap(name: String, heightDp: Int = tallDp, prepare: () -> Unit = {}, content: @Composable () -> Unit) {
        currentHeightDp = heightDp
        current = content
        prepare()
        val scale = (fontScale * 100).toInt()
        val dir = "${roborazziSystemPropertyOutputDirectory()}/${device.name}/fs$scale"
        compose.onNodeWithTag(SHOT).captureRoboImage("$dir/$name.png")
        // Befunde neben dem Bild ablegen (überschreibt den letzten Lauf); Übersicht per
        // `cat app/build/outputs/roborazzi/*/*/*.tsv`.
        val findings = textBreaks(name)
        val controls = controlFindings(name)
        // Ohne Aufnahme (normaler Testlauf) legt Roborazzi den Ordner nicht an.
        File(dir).mkdirs()
        File("$dir/$name.tsv").writeText((findings + controls).joinToString("") { "$it\n" })
        wordBreaks += findings
        controlProblems += controls
        current = null
    }

    private val wordBreaks = mutableListOf<String>()
    private val controlProblems = mutableListOf<String>()

    /** LAYOUT-03: Kein Wort bricht um, kein Text wird gekürzt oder angeschnitten – auf keiner Breite, bei keiner Schriftgröße. */
    @After
    fun noWordBreaks() {
        if (wordBreaks.isNotEmpty()) {
            fail("Text bricht im Wort um, wird gekürzt oder angeschnitten (LAYOUT-03):\n" + wordBreaks.joinToString("\n"))
        }
    }

    /** A11Y-05, A11Y-02, LAYOUT-05: Tippflächen ≥ 48 dp, Klick-Aktion vorhanden, nichts abgeschnitten. */
    @After
    fun controlsWhole() {
        if (controlProblems.isNotEmpty()) {
            fail("Bedienelement zu klein, ohne Klick-Aktion oder abgeschnitten (A11Y-05, A11Y-02, LAYOUT-05):\n" + controlProblems.joinToString("\n"))
        }
    }

    /** Bedienelemente des aktuellen Bildes prüfen ([ControlFindings]). */
    private fun controlFindings(screen: String): List<String> {
        val nodes = compose.onAllNodes(SemanticsMatcher("alle") { true }, useUnmergedTree = true).fetchSemanticsNodes()
        val frame = compose.onNodeWithTag(SHOT).fetchSemanticsNode().boundsInRoot
        return ControlFindings.findings(nodes, frame, compose.density.density, screen)
            .map { "${device.name}\tfs${(fontScale * 100).toInt()}\t$it" }
    }

    /**
     * Liest von jedem gerenderten Text das Layout aus und meldet
     * - `WORTBRUCH`: Umbruch mitten im Wort oder vor Bindestrich/Satzzeichen (`badBreakAt`),
     * - `GETRENNT`: dasselbe mit `Hyphens.Auto` (Trennstrich gesetzt; zur Durchsicht),
     * - `ABGESCHNITTEN`: Text per Ellipse gekürzt oder über maxLines/Höhe hinaus ausgeblendet,
     * - `CHIP_UMBRUCH`: Chip-Text über mehr als eine Zeile (z. B. zweiter Chip zusammengedrückt),
     * - `ZUGESCHNITTEN`: eine runde Form (clip) schneidet Glyphen an ([TextClipFindings]).
     */
    private fun textBreaks(screen: String): List<String> {
        val nodes = compose.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        return nodes.flatMap { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            layouts.flatMap { layout ->
                val text = layout.layoutInput.text.text
                val hyphenated = layout.layoutInput.style.hyphens == Hyphens.Auto
                val breaks = (0 until layout.lineCount - 1).mapNotNull { line ->
                    val end = layout.getLineEnd(line)
                    if (!badBreakAt(text, end)) return@mapNotNull null
                    val kind = if (hyphenated) "GETRENNT" else "WORTBRUCH"
                    "$kind\t$screen\t…${text.substring(maxOf(0, end - 30), end)}|${text.substring(end, minOf(text.length, end + 20))}…"
                }
                // Nicht hasVisualOverflow: das meldet schon Subpixel-Überhang einzelner Glyphen
                // („M“ in den Wochentagen), der nicht sichtbar ist. Gezählt wird nur Text, der
                // per Ellipse gekürzt oder durch maxLines/Höhe ganz ausgeblendet wird.
                val last = layout.lineCount - 1
                val truncated = last >= 0 &&
                    (layout.isLineEllipsized(last) || layout.getLineEnd(last, visibleEnd = true) < text.trimEnd().length)
                val clipped = if (truncated) listOf("ABGESCHNITTEN\t$screen\t$text") else emptyList()
                // Chips bleiben einzeilig; zu viele Chips brechen zwischen den Chips um (TitleWithChips).
                val chipWrap = node.config.getOrNull(SemanticsProperties.TestTag) == CHIP_TEXT_TAG && layout.lineCount > 1
                breaks + clipped + if (chipWrap) listOf("CHIP_UMBRUCH\t$screen\t$text") else emptyList()
            } + TextClipFindings.findings(node).map { "ZUGESCHNITTEN\t$screen\t$it" }
        }.map { "${device.name}\tfs${(fontScale * 100).toInt()}\t$it".replace("\n", "⏎") }
    }

    @Test
    fun home() = snap("01_atmen") { Home(dailySaved = false) }

    /** Lesezeichen im Zustand „gespeichert“: gefüllt im Akzent, Kartenhöhe unverändert. */
    @Test
    fun homeDailySaved() = snap("01_atmen_gespeichert", heightDp = device.heightDp) { Home(dailySaved = true) }

    @Composable
    private fun Home(dailySaved: Boolean) {
        HomeScreen(
            exercisesByFamily = BreathingFamily.entries
                .map { f -> f to BuiltInExercises.all.filter { it.family == f && it.guided } }
                .filter { (_, list) -> list.isNotEmpty() },
            programs = BuiltInExercises.all.filter { !it.guided },
            daily = daily,
            dailySaved = dailySaved,
            onRegenerateDaily = {},
            onDailySavedChange = {},
            onSelect = {},
            onOpenSettings = {},
        )
    }

    /** Befindens-Übersicht: alle Situationen zu (Standard); zwei Muster stehen offen, ohne Caret. */
    @Test
    fun situations() = snap("02_situationen") { SituationsContent() }

    /** Drei Muster: „Meine Muster · 3“ einklappbar, Standard zu. */
    @Test
    fun situationsPatternsCollapsed() = snap("02_situationen_muster_zu", heightDp = device.heightDp) {
        SituationsContent(patterns = 3)
    }

    /** Drei Muster, vom Nutzer aufgeklappt: Caret nach oben, Karten darunter. */
    @Test
    fun situationsPatternsExpanded() = snap("02_situationen_muster_auf", heightDp = device.heightDp) {
        SituationsContent(patterns = 3, patternsExpanded = true)
    }

    /** Eine Situation aufgeklappt – die mit den meisten Übungen, Caret zeigt nach oben. */
    @Test
    fun situationsExpanded() {
        val widest = Situations.all.maxBy { it.exerciseIds.size }
        snap("02_situationen_aufgeklappt", prepare = {
            compose.onNodeWithText(widest.title).performClick()
        }) { SituationsContent() }
    }

    /** Befindens-Suche offen, noch leer: Feld mit Platzhalter statt Untertitel, X rechts. */
    @Test
    fun situationsSearchEmpty() = snap("02_situationen_suche", heightDp = device.heightDp, prepare = { openSearch() }) { SituationsContent() }

    /** Suche mit zwei Treffern: aufgeklappt, beste zuerst, Trefferzahl darüber. */
    @Test
    fun situationsSearchHits() = snap("02_situationen_suche_treffer", prepare = {
        openSearch()
        compose.onNode(hasSetTextAction()).performTextInput("Angst vor der Prüfung")
    }) { SituationsContent() }

    /** Suche ohne Treffer: „Nichts gefunden“ mit „Alle zeigen“. */
    @Test
    fun situationsSearchNone() = snap("02_situationen_suche_leer", heightDp = device.heightDp, prepare = {
        openSearch()
        compose.onNode(hasSetTextAction()).performTextInput("Asthma")
    }) { SituationsContent() }

    private fun openSearch() {
        compose.onNodeWithContentDescription(RuntimeEnvironment.getApplication().getString(R.string.cd_search_open)).performClick()
    }

    @Composable
    private fun SituationsContent(patterns: Int = 2, patternsExpanded: Boolean = false) {
        SituationsScreen(
            recommendations = Situations.all,
            savedPatterns = saved.take(patterns),
            savedPatternsExpanded = patternsExpanded,
            onSavedPatternsExpandedChange = {},
            resolve = { id ->
                BuiltInExercises.all.firstOrNull { it.id == id }
                    ?: saved.firstOrNull { it.exercise.id == id }?.exercise
            },
            onSelect = {},
            onDeleteSaved = {},
        )
    }

    /** Zeitring auf 10 min (Standard): Zahl 10 in Cyan, Seite füllt die Gerätehöhe ohne Scrollen. */
    @Test
    fun meditation() = snap("03_meditation", heightDp = device.heightDp) {
        MeditationTab(MeditationConfig())
    }

    /** 12 min (zwischen zwei Zahlen: Cyan-Label am Knopf); Reset im Split-Button sichtbar (gleiche Höhe wie Start). */
    @Test
    fun meditationReset() = snap("03_meditation_reset", heightDp = device.heightDp) {
        MeditationTab(MeditationConfig(minutes = 12))
    }

    /** Modus „Frei“ (∞): Bogen voll in Pink; „Klang & Stimme“ aufgeklappt mit Gong „alle N min“ (längstes Segment) – Ring am kleinsten. */
    @Test
    fun meditationFree() = snap("03_meditation_frei", heightDp = device.heightDp, prepare = {
        val app = RuntimeEnvironment.getApplication()
        compose.onNodeWithText(app.getString(R.string.meditation_sound_voice_label)).performClick()
    }) {
        MeditationTab(MeditationConfig(mode = MeditationMode.FREE, gongEveryMin = 15), gongIntervalMin = 15)
    }

    /** Feinwahl offen: Tipp auf die Zeit in der Mitte blendet − und + neben der Zahl ein. */
    @Test
    fun meditationFine() = snap("03_meditation_fein", heightDp = device.heightDp, prepare = {
        val app = RuntimeEnvironment.getApplication()
        // Die Mitte ist klickbar und meldet ihren Zustand als „Feinwahl aus“.
        val closed = app.getString(R.string.meditation_dial_fine_off)
        compose.onNode(
            SemanticsMatcher("Mitte des Zeitrings") { node ->
                node.config.getOrNull(SemanticsActions.OnClick) != null &&
                    node.config.getOrNull(SemanticsProperties.StateDescription) == closed
            },
        ).performClick()
    }) {
        MeditationTab(MeditationConfig(minutes = 25))
    }

    @Composable
    private fun MeditationTab(config: MeditationConfig, gongIntervalMin: Int = 5) {
        MeditationScreen(
            initialConfig = config,
            speechAvailable = true,
            gongIntervalMin = gongIntervalMin,
            onOpenSettings = {},
            onDurationTick = {},
            onStart = {},
        )
    }

    @Test
    fun logbook() = snap("04_logbuch") {
        // Relativ zum festen DAY, damit jede Gruppe vorkommt: Heute, Gestern, Diese Woche, zwei Monate;
        // ein Tag mit zwei Sitzungen.
        fun at(daysAgo: Long, hour: Int, minute: Int): Long = DAY.minusDays(daysAgo).atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val daysAgo = listOf(0L, 1L, 3L, 5L, 12L, 40L)
        val entries = BuiltInExercises.all.take(6).mapIndexed { i, e ->
            SessionLogEntry(
                id = i + 1L,
                exerciseId = e.id,
                exerciseName = e.name,
                family = e.family,
                startedAtEpochMs = at(daysAgo[i], 7, 30),
                durationMs = (5 + i) * 60_000L + 5_000L,
                roundsCompleted = 4 + i,
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
        LogbookScreen(entries = entries, onOpenSettings = {}, today = DAY)
    }

    @Test
    fun logbookEmpty() = snap("04_logbuch_leer", heightDp = device.heightDp) {
        LogbookScreen(entries = emptyList(), onOpenSettings = {}, today = DAY)
    }

    @Test
    fun settings() = snap("05_einstellungen") { Settings() }

    /** Vom Meditations-Tab geöffnet (Zahnrad): in Gerätehöhe, direkt bei der Meditations-Karte. */
    @Test
    fun settingsFromMeditation() = snap("05_einstellungen_meditation", heightDp = device.heightDp) {
        Settings(focusMeditation = true)
    }

    /** In Gerätehöhe ganz nach unten gescrollt: Pfeil und Titel stehen fest im Kopf (PushHeader). */
    @Test
    fun settingsScrolled() = snap("05_einstellungen_gescrollt", heightDp = device.heightDp, prepare = {
        compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, Float.MAX_VALUE) }
    }) { Settings() }

    /** Daten-Karte mit den längsten Rückmeldungen (Button-Labels + Hinweis) – LAYOUT-03. */
    @Test
    fun settingsDataStatus() = snap("05_einstellungen_daten") {
        Settings(
            dataStatus = DataStatus(
                exportLabel = pluralStringResource(R.plurals.data_export_done, 120, 120),
                importLabel = pluralStringResource(R.plurals.data_import_done, 120, 120, 380),
                note = stringResource(R.string.data_import_not_backup),
            ),
        )
    }

    /**
     * „Logbuch leeren“ nach dem ersten Tipp (MUSTER-05): Label „Wirklich leeren?“ in error. Die
     * Uhr steht nach dem Tipp, sonst liefe der 4-s-Rückfall vor der Aufnahme ab.
     */
    @Test
    fun settingsClearLogArmed() = snap("05_einstellungen_logbuch_leeren", prepare = {
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText(RuntimeEnvironment.getApplication().getString(R.string.settings_data_clear_log)).performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
    }) { Settings() }

    @Composable
    private fun Settings(focusMeditation: Boolean = false, dataStatus: DataStatus = DataStatus()) {
        val voices = VoiceCatalog.all.mapIndexed { i, v ->
            v.id to when (i) {
                0 -> VoiceDownloadState.Downloaded
                1 -> VoiceDownloadState.Downloading(fraction = 0.4f)
                else -> VoiceDownloadState.NotDownloaded
            }
        }.toMap()
        SettingsScreen(
            soundMode = SoundMode.entries.first(),
            transition = TransitionEmphasis.entries.first(),
            volume = ToneVolume.entries.first(),
            haptics = true,
            showSafetyWarning = true,
            showNextPhase = true,
            onSoundMode = {},
            onTransition = {},
            onVolume = {},
            onToggleHaptics = {},
            onToggleSafety = {},
            onToggleNextPhase = {},
            gongIntervalMin = 5,
            onGongInterval = {},
            gongLong = false,
            onGongLong = {},
            onPreviewGong = {},
            voiceStates = voices,
            activeVoiceId = VoiceCatalog.all.first().id,
            piperEngineReady = true,
            onSampleVoice = {},
            onDownloadVoice = {},
            onSelectVoice = {},
            onDeleteVoice = {},
            onOpenGlossary = {},
            onOpenAtemlehre = {},
            onOpenAbout = {},
            onBack = {},
            onExportPatterns = {},
            onImportPatterns = {},
            onClearLog = {},
            dataStatus = dataStatus,
            focusMeditation = focusMeditation,
        )
    }

    @Test
    fun glossary() = snap("06_glossar") { GlossaryScreen(onBack = {}) }

    @Test
    fun about() = snap("07_ueber") { AboutScreen(onBack = {}) }

    /** Über-Screen mit aufgeklappten Lizenzen: Bausteine, Stimmen, Links mit 48-dp-Zeilen. */
    @Test
    fun aboutLicenses() = snap("07_ueber_lizenzen") { AboutScreen(onBack = {}, licensesExpanded = true) }

    /**
     * Jede eingebaute Übung plus Muster des Tages: Name im festen Kopf (neben der Markierung wie
     * in der App, beim Muster ohne), darunter Familie und Chips.
     */
    @Test
    fun details() {
        assumeTrue(detailConfig)
        (BuiltInExercises.all + daily.exercise).forEach { exercise ->
            snap("08_detail_${exercise.id}") {
                ExerciseDetailScreen(
                    exercise = exercise,
                    requireSafetyConfirm = false,
                    savedIntervals = null,
                    onIntervalsChange = { _, _, _, _ -> },
                    onIntervalsReset = {},
                    onConfirmedSafety = {},
                    onStart = {},
                    onBack = {},
                    onOpenLore = {},
                    onMarkedChange = if (exercise in BuiltInExercises.all) ({ _: Boolean -> }) else null,
                )
            }
        }
    }

    /** Detailseite mit aufgeklapptem Abschnitt „Aus der Yoga-Lehre“: Detail, Quelle, Verweis zur Atemlehre. */
    @Test
    fun detailLore() {
        assumeTrue(detailConfig)
        val exercise = BuiltInExercises.all.first { it.id == "bhramari" }
        snap("08_detail_lehre_bhramari", prepare = {
            compose.onNodeWithText(RuntimeEnvironment.getApplication().getString(R.string.detail_lore)).performClick()
        }) {
            ExerciseDetailScreen(
                exercise = exercise,
                requireSafetyConfirm = false,
                savedIntervals = null,
                onIntervalsChange = { _, _, _, _ -> },
                onIntervalsReset = {},
                onConfirmedSafety = {},
                onStart = {},
                onBack = {},
                onOpenLore = {},
            )
        }
    }

    /** Tagesmuster mit beiden Halten: Lehre aufgeklappt, drei Verweise (Verhältnis, Halten voll, Halten leer). */
    @Test
    fun detailLoreDaily() {
        assumeTrue(detailConfig)
        val exercise = (0L until 200L).map(RandomPatternGenerator::forSeed).first { it.spec.holdEmpty != null }.exercise
        snap("08_detail_lehre_tagesmuster", prepare = {
            compose.onNodeWithText(RuntimeEnvironment.getApplication().getString(R.string.detail_lore)).performClick()
        }) {
            ExerciseDetailScreen(
                exercise = exercise,
                requireSafetyConfirm = false,
                savedIntervals = null,
                onIntervalsChange = { _, _, _, _ -> },
                onIntervalsReset = {},
                onConfirmedSafety = {},
                onStart = {},
                onBack = {},
                onOpenLore = {},
            )
        }
    }

    /**
     * Detailseite mit gespeicherter Anpassung: Reset sichtbar, Intervalle aufgeklappt – Stepper
     * in der Karte und Split-Button auf jeder Breite und Schriftgröße.
     */
    @Test
    fun detailAdjusted() {
        val exercise = BuiltInExercises.all.first { it.id == "4-7-8" }
        snap("08_detail_angepasst", prepare = {
            compose.onNodeWithText(RuntimeEnvironment.getApplication().getString(R.string.adjust_intervals)).performClick()
        }) {
            ExerciseDetailScreen(
                exercise = exercise,
                requireSafetyConfirm = false,
                savedIntervals = IntervalOverrides(duration = 6, inhale = 5, hold = 8, exhale = 9),
                onIntervalsChange = { _, _, _, _ -> },
                onIntervalsReset = {},
                onConfirmedSafety = {},
                onStart = {},
                onBack = {},
            )
        }
    }

    /** Vollbild-Session in echter Gerätehöhe: Vorbereitung, Phase mit Notiz, offene Phase, Ende. */
    @Test
    fun session() {
        val exercise = BuiltInExercises.all.first { it.guided }
        val base = SessionUiState(
            exerciseName = exercise.name,
            status = SessionStatus.RUNNING,
            roundIndex = 2,
            roundCount = 8,
            patternHint = "Einatmen 4 s · Halten 7 s · Ausatmen 8 s",
        )
        val states = mapOf(
            "vorbereitung" to base.copy(status = SessionStatus.PREPARING, countdown = 3),
            "einatmen" to base.copy(
                phaseType = PhaseType.INHALE,
                phaseNote = "Durch die Nase, Bauch weitet sich",
                remainingMs = 2_400,
                phaseTotalMs = 4_000,
                nextPhaseType = PhaseType.HOLD_FULL,
            ),
            // Viloma: Pause nach der ersten Einatemstufe – Kreis steht bei ⅓.
            "viloma_pause" to base.copy(
                phaseType = PhaseType.HOLD_FULL,
                phaseLevel = BreathLevel.at(1f / 3f),
                phaseNote = "anhalten",
                remainingMs = 1_200,
                phaseTotalMs = 2_000,
                nextPhaseType = PhaseType.INHALE,
            ),
            "warten" to base.copy(
                status = SessionStatus.WAITING_FOR_USER,
                phaseType = PhaseType.HOLD_EMPTY,
                elapsedMs = 23_000,
            ),
            "ende" to base.copy(status = SessionStatus.FINISHED),
        )
        states.forEach { (name, state) ->
            snap("09_session_$name", heightDp = device.heightDp) {
                SessionOverlay(
                    state = state,
                    showNextPhase = true,
                    onMinimize = {},
                    onToggleMute = {},
                    onTogglePause = {},
                    onTapHaptic = {},
                    onContinue = {},
                    onRestart = {},
                    onStop = {},
                )
            }
        }
    }

    /** Laufende Meditation in Gerätehöhe: Vorbereitung, Timer läuft, Ende. */
    @Test
    fun meditationSession() {
        val states = mapOf(
            "vorbereitung" to MeditationUiState(status = MeditationStatus.PREPARING, countdown = 3),
            "laeuft" to MeditationUiState(status = MeditationStatus.RUNNING, remainingMs = 7 * 60_000L + 12_000L, elapsedMs = 2 * 60_000L + 48_000L),
            "ende" to MeditationUiState(status = MeditationStatus.FINISHED),
        )
        states.forEach { (name, state) ->
            snap("10_meditation_$name", heightDp = device.heightDp) {
                MeditationOverlay(
                    state = state,
                    onMinimize = {},
                    onToggleMute = {},
                    onTogglePause = {},
                    onTapHaptic = {},
                    onRestart = {},
                    onEnd = {},
                )
            }
        }
    }

    /** Atemlehre: alle Karten zu, Rhythmusbilder und Teaser ganz. */
    @Test
    fun atemlehre() = snap("12_atemlehre") { AtemlehreScreen(onBack = {}, onOpenExercise = {}) }

    /** Atemlehre mit aufgeklappten Köpfen: Grundregeln, Weitere Einflüsse (Chips), Quellen. */
    @Test
    fun atemlehreOffen() = snap("12_atemlehre_offen", prepare = {
        val app = RuntimeEnvironment.getApplication()
        listOf(R.string.atemlehre_rules, R.string.atemlehre_influences, R.string.atemlehre_sources).forEach {
            compose.onNodeWithText(app.getString(it)).performClick()
        }
    }) { AtemlehreScreen(onBack = {}, onOpenExercise = {}) }

    /** Sprung aus der Detailseite: Karte „Feuer“ aufgeklappt (Lehre, Studien, Vorsicht-Chip, Warnung). */
    @Test
    fun atemlehreFokus() = snap("12_atemlehre_fokus") {
        AtemlehreScreen(onBack = {}, onOpenExercise = {}, focus = "feuer")
    }

    /** Untere Navigationsleiste (liegt in MainActivity, hier einzeln): Beschriftungen ganz. */
    @Test
    fun navigationBar() = snap("11_navigation", heightDp = 120) {
        AppNavigationBar(
            listOf(
                R.string.tab_breathe to Icons.Filled.Home,
                R.string.tab_situations to Icons.Filled.Search,
                R.string.tab_meditation to Icons.Filled.Favorite,
                R.string.tab_logbook to Icons.Filled.DateRange,
            ).mapIndexed { i, (label, icon) ->
                AppNavItem(label = stringResource(label), selected = i == 1, onClick = {}) {
                    Icon(icon, contentDescription = null)
                }
            },
        )
    }
}
