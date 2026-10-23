package app.atemkraft.ui.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import app.atemkraft.R
import app.atemkraft.data.SavedPattern
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.FreePhase
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.domain.GeneratedExercises
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.PatternOrigin
import app.atemkraft.domain.PatternSpec
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.ui.SilentPreviewSound
import app.atemkraft.ui.TestMatrix.Device
import app.atemkraft.ui.components.CHIP_TEXT_TAG
import app.atemkraft.ui.components.MARQUEE_TEXT_TAG
import app.atemkraft.ui.components.badBreakAt
import app.atemkraft.ui.meditation.MeditationScreen
import app.atemkraft.ui.navigation.AppTabBar
import app.atemkraft.ui.navigation.HideableTabBar
import app.atemkraft.ui.navigation.TopTab
import app.atemkraft.ui.patterns.PATTERN_INFO_TAG
import app.atemkraft.ui.patterns.PATTERN_TOOLBAR_TAG
import app.atemkraft.ui.patterns.PatternEditorScreen
import app.atemkraft.ui.patterns.PatternEditorState
import app.atemkraft.ui.patterns.curve.CurveAxisSeconds
import app.atemkraft.ui.patterns.curve.CurveHeightRange
import app.atemkraft.ui.patterns.rememberPatternEditorState
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.DarkSurface
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.github.takahirom.roborazzi.roborazziSystemPropertyOutputDirectory
import kotlinx.coroutines.awaitCancellation
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/** Tabs der unteren Leiste: dieselbe Liste wie in der App ([TopTab]). */
internal typealias Tab = TopTab

/**
 * Gemeinsamer Rahmen der Screenshot-Gates ([ScreenshotTest] über die Breiten, [DeviceFitTest]
 * über die Gerätehöhen): Aufnahme, [TabFrame] und die Prüfungen, die bei jedem Befund fehlschlagen.
 *
 * Screenshots aller Screens auf typischen Gerätebreiten und Schriftgrößen (A-12, LAYOUT-03).
 * Läuft auf der JVM (Robolectric), kein Gerät nötig. Listen-Screens werden mit großer Höhe
 * gerendert, damit der gesamte Scroll-Inhalt auf einem Bild steht; Vollbild-Sessions in
 * echter Gerätehöhe. Screens in Gerätehöhe, die in der App im Scaffold liegen, bekommen die
 * Leisten der App mit ([TabFrame]): Statusleiste, Tab-Leiste und Systemnavigation – sonst
 * überschätzt das Bild den Platz um rund 150 dp.
 *
 * Der Meditations-Tab scrollt bis fontScale 1,3 nicht (LAYOUT-08, `SCROLLT`); „Klang & Stimme“
 * liegt im Bottom Sheet und ändert die Seite nicht.
 *
 * Endlos-Animationen stehen still (FrozenInfiniteAnimations).
 *
 * Bilder erzeugen: `scripts/build.sh recordRoborazziDebug` → `app/build/outputs/roborazzi/`.
 */
abstract class ScreenshotHarness(protected val device: Device, protected val fontScale: Float) {

    companion object {
        internal const val SHOT = "screenshot"

        /** Höhe für Listen-Screens bei fontScale 1,0; wächst mit der Schrift. */
        private const val TALL_DP = 4000

        // Leisten um den Scaffold-Inhalt (Robolectric liefert keine echten Insets, daher Annahmen
        // für den ungünstigen Fall): Statusleiste und 3-Tasten-Navigation, wie oft bei Samsung.
        // Tab-Leiste (80 dp) und Trennlinie rendert TabFrame echt.
        private const val STATUS_BAR_DP = 24
        private const val SYSTEM_NAV_DP = 48

        /** Bis zu dieser Schriftgröße prüft der Muster-Editor hart, dass Kurve und Werkzeugleiste ohne Scrollen dastehen. */
        private const val COMPACT_EDITOR_MAX_FONT_SCALE = 1.3f

        /** Bottom Sheets (Vorlagen, Klang & Stimme): eigenes Fenster, also Fenster in Gerätegröße wie die Dialoge ([setUp]). */
        private val SHEET_TESTS = listOf("patternEditorTemplates", "meditationSoundSheet")

        /** Bis zu dieser Schriftgröße scrollt der Meditations-Tab nicht; bei 2,0 darf er (Notlösung). */
        private const val NO_SCROLL_MAX_FONT_SCALE = 1.3f

        // Feste Werte statt „heute“, damit die Bilder reproduzierbar sind.
        internal val DAY = LocalDate.of(2026, 10, 3)
        internal val daily = RandomPatternGenerator.forDate(DAY)
        internal val saved = listOf(11L, 42L, 7L).map(RandomPatternGenerator::forSeed)
            .mapIndexed { i, p -> SavedPattern(id = i + 1L, exercise = p.exercise, spec = p.spec) } +
            customPattern(id = 4L)

        /** Ein im Editor frei gestaltetes Muster (Ein 4, Halten 1, Aus 6) mit langem, frei gewähltem Namen. */
        private fun customPattern(id: Long): SavedPattern {
            val pattern = FreePattern(listOf(FreePhase(FreePhaseType.IN, 4), FreePhase(FreePhaseType.HOLD, 1), FreePhase(FreePhaseType.OUT, 6)))
            val name = "Abendruhe nach dem Spaziergang am See"
            return SavedPattern(
                id = id,
                exercise = GeneratedExercises.exerciseFrom(pattern, "muster-$id", name, minutes = 5),
                spec = PatternSpec(4.0, 1.0, 6.0, null, activating = false),
                origin = PatternOrigin.CUSTOM,
                minutes = 5,
                pattern = pattern,
            )
        }

        /** Vorlage „Startpunkt“ nach ihrer Beschriftung. */
        internal fun template(label: String) = FreePatternTemplates.all.single { it.label == label }.pattern
    }

    protected val tallDp get() = (TALL_DP * fontScale).toInt()

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

    /** Fenster (Breite, Höhe in dp) der Dialog-Aufnahmen: Gerätegröße. */
    protected val windowDp get() = device.widthDp to device.heightDp

    @Before
    fun setUp() {
        // Dialoge messen sich am Fenster, nicht am Testknoten: Für sie Gerätegröße statt des
        // hohen Testfensters. Vor setContent, weil der Wechsel die Activity neu erzeugt.
        if (testName.methodName.startsWith("safetyDialog") || SHEET_TESTS.any(testName.methodName::startsWith)) {
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

    /**
     * [prepare] läuft nach dem Setzen des Inhalts und vor der Aufnahme (z. B. etwas aufklappen).
     * Mit [tab] liegt der Inhalt wie in der App über Tab-Leiste und Systemleisten ([TabFrame],
     * dann in Gerätehöhe bzw. [frameHeightDp]); [noScroll] prüft, dass die Seite bis
     * [NO_SCROLL_MAX_FONT_SCALE] nicht scrollt.
     * [screen] nimmt den ganzen Bildschirm samt eigener Fenster auf (Dialoge); das Fenster muss
     * dafür Gerätegröße haben ([deviceWindow]).
     * [export] bekommt das Bild des Testknotens unabhängig vom Roborazzi-Modus (Store-Bilder,
     * [StoreScreenshotTest]).
     */
    @OptIn(ExperimentalRoborazziApi::class)
    protected fun snap(
        name: String,
        heightDp: Int = tallDp,
        widthDp: Int = device.widthDp,
        tab: Tab? = null,
        tabBar: Boolean = true,
        frameHeightDp: Int = device.heightDp,
        noScroll: Boolean = false,
        screen: Boolean = false,
        prepare: () -> Unit = {},
        measure: () -> List<String> = { emptyList() },
        export: ((ImageBitmap) -> Unit)? = null,
        content: @Composable () -> Unit,
    ) {
        currentWidthDp = widthDp
        currentHeightDp = if (tab == null) heightDp else frameHeightDp
        current = if (tab == null) content else ({ TabFrame(tab, tabBar, content) })
        prepare()
        val scale = (fontScale * 100).toInt()
        val dir = "${roborazziSystemPropertyOutputDirectory()}/${device.name}/fs$scale"
        if (screen) {
            compose.waitForIdle()
            captureScreenRoboImage("$dir/$name.png")
        } else {
            compose.onNodeWithTag(SHOT).captureRoboImage("$dir/$name.png")
        }
        export?.invoke(compose.onNodeWithTag(SHOT).captureToImage())
        // Befunde neben dem Bild ablegen (überschreibt den letzten Lauf); Übersicht per
        // `cat app/build/outputs/roborazzi/*/*/*.tsv`.
        val findings = textBreaks(name)
        val controls = controlFindings(name)
        val checked = fontScale <= NO_SCROLL_MAX_FONT_SCALE
        val scrolls = if (noScroll && checked) scrollFindings(name) else emptyList()
        val measured = measure()
        // Ohne Aufnahme (normaler Testlauf) legt Roborazzi den Ordner nicht an.
        File(dir).mkdirs()
        File("$dir/$name.tsv").writeText((findings + controls + scrolls + measured).joinToString("") { "$it\n" })
        wordBreaks += findings
        controlProblems += controls
        scrollProblems += scrolls
        current = null
    }

    /**
     * Rahmen wie im Scaffold der App: Statusleiste oben, darunter der Inhalt im Rest, unten
     * Trennlinie und [AppTabBar] mit [selected] markiert – ohne [tabBar] eingeklappt wie eine
     * Seite der Tiefe ([HideableTabBar]; der Knopf dazu schwebt über dem Inhalt) –, darunter die
     * Systemnavigation (in der App reicht die Leistenfläche bis dorthin).
     */
    @Composable
    private fun TabFrame(selected: Tab, tabBar: Boolean, content: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Spacer(Modifier.height(STATUS_BAR_DP.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            // Wie der Rahmen der App (Scaffold-bottomBar); die Systemnavigation steht hier darunter.
            HideableTabBar(visible = tabBar, systemInsets = WindowInsets(0)) { TabNavigation(selected) }
            Spacer(Modifier.fillMaxWidth().height(SYSTEM_NAV_DP.dp).background(DarkSurface))
        }
    }

    /** Die Tab-Leiste der App ([AppTabBar] aus [TopTab]: Icons, Beschriftungen, Reihenfolge). */
    @Composable
    private fun TabNavigation(selected: Tab) {
        AppTabBar(selected = selected, onTap = {})
    }

    private val wordBreaks = mutableListOf<String>()
    private val controlProblems = mutableListOf<String>()
    private val scrollProblems = mutableListOf<String>()
    private val compactProblems = mutableListOf<String>()

    /**
     * MUSTER-10 (Nachtrag 5): Im Muster-Editor stehen Kopf, Werte, Kurve, Vorschau und Werkzeugleiste
     * (gewählte Phase mit Stepper und Papierkorb, + Ein / + Halten / + Aus) ohne Scrollen ganz im
     * Bild – auf jedem Gerät der Matrix bis fontScale [COMPACT_EDITOR_MAX_FONT_SCALE] hart geprüft,
     * ohne Tab-Leiste (Tiefe, Nutzer 2026-10-23); bei 2,0 nur gemessen. Überall ein Fehler: Die
     * Seite scrollt, obwohl die Mindesthöhe der Kurve gepasst hätte, oder die Kurve ist nicht so
     * hoch, wie der Platz erlaubt.
     */
    @After
    fun editorCompact() {
        if (compactProblems.isNotEmpty()) {
            fail("Muster-Editor: Kurve oder Werkzeugleiste nicht ohne Scrollen sichtbar (MUSTER-10):\n" + compactProblems.joinToString("\n"))
        }
    }

    /**
     * Misst im Muster-Editor, wie weit Kurve und Werkzeugleiste in den sichtbaren Teil der
     * Scroll-Fläche reichen, und die Höhe der Kurve gegen ihre Spanne (Mindest- bis volle Höhe):
     * Befund `KOMPAKT` (passt), `KOMPAKT_FEHLT` als Fehler ([compactProblems]) oder
     * `KOMPAKT_SCROLLT` (nur gemessen). Fehler, wo [hard]: Es passt nicht, oder die Kurve ist nicht
     * so hoch wie der freie Platz erlaubt (geklemmt auf ihre Spanne). Ohne [hard] ist es immer noch
     * ein Fehler, wenn die Seite scrollt, obwohl die Mindesthöhe der Kurve gepasst hätte. Die Zeile
     * nennt die gezeigte Fassung der Werte über der Kurve (MUSTER-10: kürzer statt umbrochen).
     * Alle Werte ab Oberkante der Scroll-Fläche, in dp.
     */
    protected fun editorFit(
        screen: String,
        hard: Boolean = fontScale <= COMPACT_EDITOR_MAX_FONT_SCALE,
    ): List<String> {
        // Ungeschnittene Lage (boundsInRoot wäre auf den sichtbaren Teil beschnitten).
        fun SemanticsNode.unclipped() = Rect(positionInRoot, size.toSize())
        val viewport = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNode().unclipped()
        val curveNode = compose.onNode(SemanticsMatcher.keyIsDefined(CurveAxisSeconds)).fetchSemanticsNode()
        val curve = curveNode.unclipped()
        val range = curveNode.config[CurveHeightRange]
        val toolbar = compose.onNodeWithTag(PATTERN_TOOLBAR_TAG).fetchSemanticsNode().unclipped()
        // Feste Leiste (Nachtrag 6): Play-Knopf und „+“ der Dauer auf einer Zeile?
        val app = RuntimeEnvironment.getApplication()
        val play = compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_editor_play).substringBefore(","), substring = true).fetchSemanticsNode().unclipped()
        val durationPlus = compose.onNodeWithContentDescription("${app.getString(R.string.cd_adjust_duration)}, ${app.getString(R.string.adjust_increase)}").fetchSemanticsNode().unclipped()
        val barOneLine = abs(play.center.y - durationPlus.center.y) <= 1f
        // Gezeigte Fassung der Werte über der Kurve.
        val info = compose.onNodeWithTag(PATTERN_INFO_TAG, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString { it.text.replace('\u00A0', ' ').replace("\u2060", "") }
        val density = compose.density.density
        fun dp(px: Float) = (px / density).roundToInt()
        // Platz für die Kurve: sichtbare Höhe minus alles darüber und Werkzeugleiste darunter.
        val free = viewport.height - (curve.top - viewport.top) - (toolbar.bottom - curve.bottom)
        val expected = free.coerceIn(range.start, range.endInclusive)
        val heightRight = abs(curve.height - expected) <= 1f
        val fits = curve.top >= viewport.top && toolbar.bottom <= viewport.bottom + 0.5f
        val minWouldFit = free >= range.start - 0.5f
        val kind = when {
            hard && !barOneLine -> "KOMPAKT_FEHLT"
            fits && heightRight -> "KOMPAKT"
            (minWouldFit && !fits) || !heightRight -> "KOMPAKT_FEHLT"
            hard -> "KOMPAKT_FEHLT"
            else -> "KOMPAKT_SCROLLT"
        }
        val line = "${device.name}\tfs${(fontScale * 100).toInt()}\t$kind\t$screen\tKurve ${dp(curve.top - viewport.top)}–${dp(curve.bottom - viewport.top)} dp " +
            "(Höhe ${dp(curve.height)} dp, Spanne ${dp(range.start)}–${dp(range.endInclusive)} dp, frei ${dp(free)} dp), " +
            "Werkzeugleiste bis ${dp(toolbar.bottom - viewport.top)} dp von ${dp(viewport.height)} dp (Rest ${dp(viewport.bottom - toolbar.bottom)} dp), " +
            "Leiste unten ${if (barOneLine) "einzeilig" else "zweizeilig"}, Werte „$info“"
        if (kind == "KOMPAKT_FEHLT") compactProblems += line
        return listOf(line)
    }

    /**
     * Kantenlänge des Zeitrings (der Regler mit `SetProgress`) in dp, als Befund `RING` – zum
     * Vergleich der Ringgröße über Geräte und Schriftgrößen.
     */
    protected fun meditationRing(screen: String): List<String> {
        val ring = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).fetchSemanticsNode()
        val side = (ring.size.width / compose.density.density).roundToInt()
        return listOf("${device.name}\tfs${(fontScale * 100).toInt()}\tRING\t$screen\t$side dp")
    }

    /** LAYOUT-08: Der Meditations-Tab scrollt bis fontScale 1,3 nicht. */
    @After
    fun noScroll() {
        if (scrollProblems.isNotEmpty()) {
            fail("Seite scrollt, obwohl sie in die Gerätehöhe passen muss (LAYOUT-08):\n" + scrollProblems.joinToString("\n"))
        }
    }

    /** Jede senkrechte Scroll-Fläche im Bild mit Scroll-Weg > 0 (Befund `SCROLLT`, Weg in dp). */
    private fun scrollFindings(screen: String): List<String> {
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .fetchSemanticsNodes()
        return nodes.mapNotNull { node ->
            val max = node.config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
            if (max <= 0f) null else "${device.name}\tfs${(fontScale * 100).toInt()}\tSCROLLT\t$screen\t${(max / compose.density.density).toInt()} dp"
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
                // Ausnahme (LAYOUT-03): Laufschrift-Titel enden nach ihrem Lauf mit „…“.
                val marqueeDone = node.config.getOrNull(SemanticsProperties.TestTag) == MARQUEE_TEXT_TAG && layout.lineCount == 1
                val clipped = if (truncated && !marqueeDone) listOf("ABGESCHNITTEN\t$screen\t$text") else emptyList()
                // Chips bleiben einzeilig; zu viele Chips brechen zwischen den Chips um (TitleWithChips).
                val chipWrap = node.config.getOrNull(SemanticsProperties.TestTag) == CHIP_TEXT_TAG && layout.lineCount > 1
                breaks + clipped + if (chipWrap) listOf("CHIP_UMBRUCH\t$screen\t$text") else emptyList()
            } + TextClipFindings.findings(node).map { "ZUGESCHNITTEN\t$screen\t$it" }
        }.map { "${device.name}\tfs${(fontScale * 100).toInt()}\t$it".replace("\n", "⏎") }
    }

    @Composable
    protected fun EditSaved() {
        val custom = saved.last()
        Editor { PatternEditorState.forEdit(it, custom.id, custom.pattern, custom.minutes, custom.exercise.name) }
    }

    @Composable
    protected fun Editor(create: (namePrefix: String) -> PatternEditorState) {
        PatternEditorScreen(
            state = rememberPatternEditorState(create = create),
            onSavedChange = {},
            onStart = { _, _ -> },
            onOpenLore = {},
            onBack = {},
            // Ohne echten Ton: Die Vorschau-Aufnahme spielt sonst Töne über Robolectric.
            soundOutput = { SilentPreviewSound },
        )
    }

    @Composable
    protected fun MeditationTab(config: MeditationConfig, gongIntervalMin: Int = 5) {
        MeditationScreen(
            initialConfig = config,
            speechAvailable = true,
            gongIntervalMin = gongIntervalMin,
            onOpenSettings = {},
            onDurationTick = {},
            onStart = {},
        )
    }
}
