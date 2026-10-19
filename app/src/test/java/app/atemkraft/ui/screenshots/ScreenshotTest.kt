package app.atemkraft.ui.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
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
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.RuntimePhase
import app.atemkraft.domain.SessionKind
import app.atemkraft.domain.SessionLogEntry
import app.atemkraft.domain.SoundMode
import app.atemkraft.domain.ToneVolume
import app.atemkraft.domain.TransitionEmphasis
import app.atemkraft.domain.buildTimeline
import app.atemkraft.domain.estimatedTotalSeconds
import app.atemkraft.ui.about.AboutScreen
import app.atemkraft.ui.atemlehre.AtemlehreScreen
import app.atemkraft.ui.components.AppNavItem
import app.atemkraft.ui.components.AppNavigationBar
import app.atemkraft.ui.components.CHIP_TEXT_TAG
import app.atemkraft.ui.components.MiniNowPlayingBar
import app.atemkraft.ui.components.badBreakAt
import app.atemkraft.ui.detail.ExerciseDetailScreen
import app.atemkraft.ui.glossary.GlossaryScreen
import app.atemkraft.ui.home.HomeScreen
import app.atemkraft.ui.log.LogbookScreen
import app.atemkraft.ui.meditation.MeditationOverlay
import app.atemkraft.ui.meditation.MeditationScreen
import app.atemkraft.ui.meditation.MeditationStatus
import app.atemkraft.ui.meditation.MeditationUiState
import app.atemkraft.ui.session.SafetyDialog
import app.atemkraft.ui.session.SessionOverlay
import app.atemkraft.ui.session.SessionProgress
import app.atemkraft.ui.session.SessionStatus
import app.atemkraft.ui.session.SessionUiState
import app.atemkraft.ui.session.sessionSummary
import app.atemkraft.ui.settings.DataStatus
import app.atemkraft.ui.settings.SettingsScreen
import app.atemkraft.ui.situations.SituationsScreen
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.DarkSurface
import app.atemkraft.ui.theme.NeonMagenta
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.github.takahirom.roborazzi.roborazziSystemPropertyOutputDirectory
import kotlinx.coroutines.awaitCancellation
import org.junit.After
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
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
 * echter Gerätehöhe. Screens in Gerätehöhe, die in der App im Scaffold liegen, bekommen die
 * Leisten der App mit ([TabFrame]): Statusleiste, Tab-Leiste und Systemnavigation – sonst
 * überschätzt das Bild den Platz um rund 150 dp.
 *
 * Der Meditations-Tab scrollt bis fontScale 1,3 nicht (LAYOUT-08, `SCROLLT`); aufgeklappt ist das
 * ein offener Verstoß (S-40), der nur gemessen wird (`SCROLLT_OFFEN`).
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

        // Leisten um den Scaffold-Inhalt (Robolectric liefert keine echten Insets, daher Annahmen
        // für den ungünstigen Fall): Statusleiste und 3-Tasten-Navigation, wie oft bei Samsung.
        // Tab-Leiste (80 dp) und Trennlinie rendert TabFrame echt.
        private const val STATUS_BAR_DP = 24
        private const val SYSTEM_NAV_DP = 48

        /** Bis zu dieser Schriftgröße scrollt der Meditations-Tab nicht; bei 2,0 darf er (Notlösung). */
        private const val NO_SCROLL_MAX_FONT_SCALE = 1.3f

        /** Befund für einen bekannten, offenen Scroll-Verstoß (S-40): gemessen, nicht als Fehler gewertet. */
        private const val OPEN_SCROLL = "SCROLLT_OFFEN"

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
    private var currentWidthDp by mutableIntStateOf(0)

    @get:Rule
    val testName = TestName()

    /** Fenster (Breite, Höhe in dp) der Dialog-Aufnahmen: Gerätegröße, quer getauscht. */
    private val windowDp get() =
        if (testName.methodName.startsWith("safetyDialogQuer")) device.heightDp to device.widthDp else device.widthDp to device.heightDp

    @Before
    fun setUp() {
        // Dialoge messen sich am Fenster, nicht am Testknoten: Für sie Gerätegröße statt des
        // hohen Testfensters. Vor setContent, weil der Wechsel die Activity neu erzeugt.
        if (testName.methodName.startsWith("safetyDialog")) {
            RuntimeEnvironment.setQualifiers("w${windowDp.first}dp-h${windowDp.second}dp-xhdpi")
        }
        // Schriftgröße wie in den Android-Einstellungen, über die echte Konfiguration: Ab
        // Android 14 skaliert das System nichtlinear (große Schrift wächst weniger), das bildet
        // nur der FontScaleConverter der Plattform ab, nicht ein linearer Density-Override.
        RuntimeEnvironment.setFontScale(fontScale)
        compose.setContent {
            AtemkraftTheme {
                // Breite/Höhe wie auf dem Gerät.
                Box(
                    Modifier
                        .requiredSize(currentWidthDp.dp, currentHeightDp.dp)
                        .testTag(SHOT),
                ) { current?.invoke() }
            }
        }
    }

    /** Tabs der unteren Leiste, in der Reihenfolge der App (MainActivity). */
    private enum class Tab { ATMEN, SITUATIONEN, MEDITATION, LOGBUCH }

    /**
     * [prepare] läuft nach dem Setzen des Inhalts und vor der Aufnahme (z. B. etwas aufklappen).
     * Mit [tab] liegt der Inhalt wie in der App über Tab-Leiste und Systemleisten ([TabFrame],
     * dann immer in Gerätehöhe); [noScroll] prüft, dass die Seite bis
     * [NO_SCROLL_MAX_FONT_SCALE] nicht scrollt. [openScroll] misst denselben Weg für einen
     * bekannten, noch nicht entschiedenen Verstoß (S-40): Befund `SCROLLT_OFFEN` in der `.tsv`,
     * der Test schlägt nicht fehl – so bleibt der Verstoß sichtbar, bis er behoben ist.
     * [screen] nimmt den ganzen Bildschirm samt eigener Fenster auf (Dialoge); das Fenster muss
     * dafür Gerätegröße haben ([deviceWindow]).
     */
    @OptIn(ExperimentalRoborazziApi::class)
    private fun snap(
        name: String,
        heightDp: Int = tallDp,
        widthDp: Int = device.widthDp,
        tab: Tab? = null,
        noScroll: Boolean = false,
        openScroll: Boolean = false,
        screen: Boolean = false,
        prepare: () -> Unit = {},
        content: @Composable () -> Unit,
    ) {
        currentWidthDp = widthDp
        currentHeightDp = if (tab == null) heightDp else device.heightDp
        current = if (tab == null) content else ({ TabFrame(tab, content) })
        prepare()
        val scale = (fontScale * 100).toInt()
        val dir = "${roborazziSystemPropertyOutputDirectory()}/${device.name}/fs$scale"
        if (screen) {
            compose.waitForIdle()
            captureScreenRoboImage("$dir/$name.png")
        } else {
            compose.onNodeWithTag(SHOT).captureRoboImage("$dir/$name.png")
        }
        // Befunde neben dem Bild ablegen (überschreibt den letzten Lauf); Übersicht per
        // `cat app/build/outputs/roborazzi/*/*/*.tsv`.
        val findings = textBreaks(name)
        val controls = controlFindings(name)
        val checked = fontScale <= NO_SCROLL_MAX_FONT_SCALE
        val scrolls = if (noScroll && checked) scrollFindings(name) else emptyList()
        val openScrolls = if (openScroll && checked) scrollFindings(name, OPEN_SCROLL) else emptyList()
        // Ohne Aufnahme (normaler Testlauf) legt Roborazzi den Ordner nicht an.
        File(dir).mkdirs()
        File("$dir/$name.tsv").writeText((findings + controls + scrolls + openScrolls).joinToString("") { "$it\n" })
        wordBreaks += findings
        controlProblems += controls
        scrollProblems += scrolls
        current = null
    }

    /**
     * Rahmen wie im Scaffold der App: Statusleiste oben, darunter der Inhalt im Rest, unten
     * Trennlinie und [AppNavigationBar] mit [selected] markiert, darunter die Systemnavigation
     * (in der App reicht die Leistenfläche bis dorthin).
     */
    @Composable
    private fun TabFrame(selected: Tab, content: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Spacer(Modifier.height(STATUS_BAR_DP.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            // Wie MainActivity (Scaffold-bottomBar).
            HorizontalDivider(thickness = 1.dp, color = NeonMagenta.copy(alpha = 0.22f))
            TabNavigation(selected)
            Spacer(Modifier.fillMaxWidth().height(SYSTEM_NAV_DP.dp).background(DarkSurface))
        }
    }

    /** Die vier Tabs wie in MainActivity (Icons und Beschriftungen). */
    @Composable
    private fun TabNavigation(selected: Tab) {
        AppNavigationBar(
            Tab.entries.map { tab ->
                val (label, icon) = when (tab) {
                    Tab.ATMEN -> R.string.tab_breathe to rememberVectorPainter(Icons.Filled.Home)
                    Tab.SITUATIONEN -> R.string.tab_situations to rememberVectorPainter(Icons.Filled.Search)
                    Tab.MEDITATION -> R.string.tab_meditation to painterResource(R.drawable.ic_meditation)
                    Tab.LOGBUCH -> R.string.tab_logbook to rememberVectorPainter(Icons.Filled.DateRange)
                }
                AppNavItem(label = stringResource(label), selected = tab == selected, onClick = {}) {
                    Icon(icon, contentDescription = null)
                }
            },
        )
    }

    private val wordBreaks = mutableListOf<String>()
    private val controlProblems = mutableListOf<String>()
    private val scrollProblems = mutableListOf<String>()

    /** LAYOUT-08: Der Meditations-Tab scrollt bis fontScale 1,3 nicht. */
    @After
    fun noScroll() {
        if (scrollProblems.isNotEmpty()) {
            fail("Seite scrollt, obwohl sie in die Gerätehöhe passen muss (LAYOUT-08):\n" + scrollProblems.joinToString("\n"))
        }
    }

    /** Jede senkrechte Scroll-Fläche im Bild mit Scroll-Weg > 0 ([kind], Weg in dp). */
    private fun scrollFindings(screen: String, kind: String = "SCROLLT"): List<String> {
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .fetchSemanticsNodes()
        return nodes.mapNotNull { node ->
            val max = node.config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
            if (max <= 0f) null else "${device.name}\tfs${(fontScale * 100).toInt()}\t$kind\t$screen\t${(max / compose.density.density).toInt()} dp"
        }
    }

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
    fun homeDailySaved() = snap("01_atmen_gespeichert", tab = Tab.ATMEN) { Home(dailySaved = true) }

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
    fun situationsPatternsCollapsed() = snap("02_situationen_muster_zu", tab = Tab.SITUATIONEN) {
        SituationsContent(patterns = 3)
    }

    /** Drei Muster, vom Nutzer aufgeklappt: Caret nach oben, Karten darunter. */
    @Test
    fun situationsPatternsExpanded() = snap("02_situationen_muster_auf", tab = Tab.SITUATIONEN) {
        SituationsContent(patterns = 3, patternsExpanded = true)
    }

    /**
     * Muster löschen nach dem ersten Tipp (MUSTER-05): Icon mit Kreuz in error, Karte gleich hoch.
     * Die Uhr steht nach dem Tipp, sonst liefe der 4-s-Rückfall vor der Aufnahme ab.
     */
    @Test
    fun situationsPatternDeleteArmed() = snap("02_situationen_muster_loeschen", tab = Tab.SITUATIONEN, prepare = {
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        val first = saved.first().exercise.name
        compose.onNodeWithContentDescription(RuntimeEnvironment.getApplication().getString(R.string.cd_pattern_delete, first)).performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
    }) { SituationsContent() }

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
    fun situationsSearchEmpty() = snap("02_situationen_suche", tab = Tab.SITUATIONEN, prepare = { openSearch() }) { SituationsContent() }

    /** Suche mit zwei Treffern: aufgeklappt, beste zuerst, Trefferzahl darüber. */
    @Test
    fun situationsSearchHits() = snap("02_situationen_suche_treffer", prepare = {
        openSearch()
        compose.onNode(hasSetTextAction()).performTextInput("Angst vor der Prüfung")
    }) { SituationsContent() }

    /** Suche ohne Treffer: „Nichts gefunden“ mit „Alle zeigen“. */
    @Test
    fun situationsSearchNone() = snap("02_situationen_suche_leer", tab = Tab.SITUATIONEN, prepare = {
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

    /** Zeitring auf 10 min (Standard): Zahl 10 in Cyan, Seite füllt die Höhe über der Tab-Leiste ohne Scrollen. */
    @Test
    fun meditation() = snap("03_meditation", tab = Tab.MEDITATION, noScroll = true) {
        MeditationTab(MeditationConfig())
    }

    /** 12 min (zwischen zwei Zahlen: Cyan-Label am Knopf); Reset im Split-Button sichtbar (gleiche Höhe wie Start). */
    @Test
    fun meditationReset() = snap("03_meditation_reset", tab = Tab.MEDITATION, noScroll = true) {
        MeditationTab(MeditationConfig(minutes = 12))
    }

    /**
     * Modus „Frei“ (∞): Bogen voll in Pink; „Klang & Stimme“ aufgeklappt mit Gong „alle N min“
     * (längstes Segment) – Ring am kleinsten. Aufgeklappt scrollt die Seite auf 360 dp schon bei
     * fontScale 1,0 – ein Verstoß gegen LAYOUT-08, keine Ausnahme; bis zur Entscheidung des
     * Eigentümers (S-40) nur gemessen (`SCROLLT_OFFEN`), nicht als Fehler gewertet.
     */
    @Test
    fun meditationFree() = snap("03_meditation_frei", tab = Tab.MEDITATION, openScroll = true, prepare = {
        val app = RuntimeEnvironment.getApplication()
        compose.onNodeWithText(app.getString(R.string.meditation_sound_voice_label)).performClick()
    }) {
        MeditationTab(MeditationConfig(mode = MeditationMode.FREE, gongEveryMin = 15), gongIntervalMin = 15)
    }

    /** Feinwahl offen: Tipp auf die Zeit in der Mitte blendet − und + neben der Zahl ein. */
    @Test
    fun meditationFine() = snap("03_meditation_fein", tab = Tab.MEDITATION, noScroll = true, prepare = {
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
        LogbookScreen(entries = entries, onOpenSettings = {}, today = DAY)
    }

    @Test
    fun logbookEmpty() = snap("04_logbuch_leer", tab = Tab.LOGBUCH) {
        LogbookScreen(entries = emptyList(), onOpenSettings = {}, today = DAY)
    }

    @Test
    fun settings() = snap("05_einstellungen") { Settings() }

    /** Vom Meditations-Tab geöffnet (Zahnrad): in Gerätehöhe, direkt bei der Meditations-Karte. */
    @Test
    fun settingsFromMeditation() = snap("05_einstellungen_meditation", tab = Tab.MEDITATION) {
        Settings(focusMeditation = true)
    }

    /** In Gerätehöhe ganz nach unten gescrollt: Pfeil und Titel stehen fest im Kopf (PushHeader). */
    @Test
    fun settingsScrolled() = snap("05_einstellungen_gescrollt", tab = Tab.ATMEN, prepare = {
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
     * Sicherheitsdialog (MUSTER-06) für beide Übungen mit `caution`. Der Dialog ist ein eigenes
     * Fenster und misst sich daran: Fenster in Gerätegröße ([setUp]), Aufnahme des ganzen
     * Bildschirms; Text- und Bedienprüfung laufen über alle Roots, also auch über das
     * Dialogfenster. Bei großer Schrift scrollt die Hinweisliste, statt still abzuschneiden.
     */
    @Test
    fun safetyDialog() = safetyDialogs("")

    /** Dasselbe quer, nur Testgerät mit Voreinstellung und kleinstes Gerät mit größter Schrift. */
    @Test
    fun safetyDialogQuer() {
        assumeTrue((device.name == "a54" && fontScale == 1.1f) || (device.name == "kompakt360" && fontScale == 2.0f))
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
     * Detailseite mit gespeicherter Anpassung: Reset sichtbar, Phasen aufgeklappt – Stepper
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

    /**
     * Vollbild-Session in echter Gerätehöhe: Vorbereitung, Phase mit Anleitungszeile, offene Phase,
     * Pause, Ruhe-Ansicht (Tipp neben den Kreis), Ende mit Bilanz (mit und ohne Runden). Alle Zustände kommen aus den Daten der
     * Übung ([sessionAt]), so wie das SessionViewModel sie setzt – keine Kombination, die es nicht gibt.
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
        // Ruhe-Ansicht: Tipp neben den Kreis (linker Rand, Bildmitte) blendet alle Bedienelemente
        // aus; Kreis, Wort, Sekunden und Bogen bleiben, nichts springt.
        snap("09_session_ruhig", heightDp = device.heightDp, prepare = {
            compose.onNodeWithTag(SHOT).performTouchInput { click(Offset(left + 4.dp.toPx(), centerY)) }
        }) { SessionShot(inhale) }
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
     * Nächstes“ ohne Überlappung. Nur Testgerät mit Voreinstellung und kleinstes Gerät mit großer
     * Schrift; Breite und Höhe getauscht (Statusleiste vernachlässigt).
     */
    @Test
    @Config(qualifiers = "w920dp-h8000dp-xhdpi")
    fun sessionQuer() {
        assumeTrue((device.name == "a54" && fontScale == 1.1f) || (device.name == "kompakt360" && fontScale == 2.0f))
        // Box-Atmung in Runde 3 von 8, Einatmen nach 1,6 s.
        val box = BuiltInExercises.all.first { it.id == "box-4-4-4-4" }
        val state = sessionAt(box, elapsedMs = 1_600) { it.roundIndex == 2 && it.type == PhaseType.INHALE }
        snap("09_session_quer", heightDp = device.widthDp, widthDp = device.heightDp) { SessionShot(state) }
        snap("10_meditation_quer", heightDp = device.widthDp, widthDp = device.heightDp) {
            MeditationOverlay(
                state = MeditationUiState(status = MeditationStatus.RUNNING, remainingMs = 7 * 60_000L + 12_000L, elapsedMs = 2 * 60_000L + 48_000L),
                onMinimize = {},
                onToggleMute = {},
                onTogglePause = {},
                onTapHaptic = {},
                onRestart = {},
                onEnd = {},
            )
        }
    }

    @Composable
    private fun SessionShot(state: SessionUiState) = SessionOverlay(
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

    /** Laufende Meditation in Gerätehöhe: Vorbereitung, Timer läuft, Pause, Ende. */
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
    fun navigationBar() = snap("11_navigation", heightDp = 120) { TabNavigation(Tab.SITUATIONEN) }

    /**
     * Mini-Leiste mit dem längsten Übungsnamen und dem längsten Phasenwort: Der Name bricht an
     * Wortgrenzen um, nichts wird gekürzt (LAYOUT-03, früher Ellipse).
     */
    @Test
    fun miniBar() = snap("11_mini_leiste", heightDp = 160) {
        val app = RuntimeEnvironment.getApplication()
        val longestPhase = listOf(R.string.phase_inhale, R.string.phase_exhale, R.string.phase_hold_full, R.string.session_paused)
            .map(app::getString)
            .maxBy { it.length }
        MiniNowPlayingBar(title = BuiltInExercises.all.maxBy { it.name.length }.name, statusText = longestPhase, onClick = {})
    }
}
