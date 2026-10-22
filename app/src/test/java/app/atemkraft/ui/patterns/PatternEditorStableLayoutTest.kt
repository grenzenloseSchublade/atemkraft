package app.atemkraft.ui.patterns

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.cue.FakePreviewSoundOutput
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.FreePhase
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.TestMatrix.Device
import app.atemkraft.ui.components.CurveAxisSeconds
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * MUSTER-10 (Nachtrag 5): Im kompakten Muster-Editor springt nichts unter dem Finger – auf allen
 * Geräten und Schriftgrößen der Matrix. Die Kurve bleibt an ihrem Platz, wenn die Pille
 * „Startpunkt“ auf „angepasst“ wechselt, wenn das Muster ins Intensive kippt (Chip „intensiv“)
 * und während die Vorschau läuft (Phasenname statt Status); die Werkzeugleiste behält ihre Höhe,
 * wenn man eine andere Phase wählt.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
// Echte Textmessung (Schriftbreiten), sonst hinge kein Umbruch von der Schrift ab.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h8000dp-xhdpi")
class PatternEditorStableLayoutTest(private val device: Device, private val fontScale: Float) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_fs{1}")
        fun params(): List<Array<Any>> = TestMatrix.DEVICES.flatMap { d -> TestMatrix.FONT_SCALES.map { arrayOf<Any>(d, it) } }

        /** Hoch genug, dass nichts scrollt: gemessen wird die Lage, nicht der sichtbare Teil. */
        private const val HEIGHT_DP = 2400

        /** Schritt durch die Vorschau von 4·1·6: jede Phase mindestens einmal. */
        private const val PREVIEW_STEP_MS = 500L
        private const val PREVIEW_STEPS = 24
    }

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val prefix get() = app.getString(R.string.pattern_editor_title)

    @Before
    fun setUp() {
        RuntimeEnvironment.setFontScale(fontScale)
    }

    private fun show(state: PatternEditorState): PatternEditorState {
        compose.setContent {
            AtemkraftTheme {
                Box(Modifier.requiredSize(device.widthDp.dp, HEIGHT_DP.dp)) {
                    PatternEditorScreen(
                        state = state,
                        onSavedChange = {},
                        onStart = { _, _ -> },
                        onOpenLore = {},
                        onBack = {},
                        soundOutput = { FakePreviewSoundOutput() },
                    )
                }
            }
        }
        return state
    }

    private fun curveTop(): Float = compose.onNode(SemanticsMatcher.keyIsDefined(CurveAxisSeconds)).fetchSemanticsNode().positionInRoot.y

    private fun toolbarHeight(): Int = compose.onNodeWithTag(PATTERN_TOOLBAR_TAG).fetchSemanticsNode().size.height

    private fun template(label: String) = FreePatternTemplates.all.single { it.label == label }.pattern

    @Test
    fun `Kurve bleibt stehen, wenn die Pille angepasst zeigt oder das Muster intensiv wird`() {
        val state = show(PatternEditorState.new(prefix, template("Box")).apply { select(1) })
        val top = curveTop()

        compose.runOnIdle { state.add(FreePhaseType.IN) }
        compose.waitForIdle()
        assertEquals("„angepasst“ in der Pille", null, state.template)
        assertEquals("Kurve nach „angepasst“", top, curveTop())

        compose.runOnIdle { state.setSeconds(1, FreePattern.INTENSE_HOLD_FULL_SECONDS + 1) }
        compose.waitForIdle()
        assertTrue(state.pattern.isIntense)
        assertEquals("Kurve mit Chip „intensiv“", top, curveTop())
    }

    @Test
    fun `Kurve bleibt stehen, waehrend die Vorschau laeuft`() {
        show(PatternEditorState.new(prefix, FreePattern(listOf(FreePhase(FreePhaseType.IN, 4), FreePhase(FreePhaseType.HOLD, 1), FreePhase(FreePhaseType.OUT, 6)))))
        val top = curveTop()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription(app.getString(R.string.cd_preview_play)).performSemanticsAction(SemanticsActions.OnClick)
        repeat(PREVIEW_STEPS) { step ->
            compose.mainClock.advanceTimeBy(PREVIEW_STEP_MS)
            assertEquals("Kurve nach ${(step + 1) * PREVIEW_STEP_MS} ms Vorschau", top, curveTop())
        }
        compose.onNodeWithContentDescription(app.getString(R.string.cd_preview_pause)).performSemanticsAction(SemanticsActions.OnClick)
        compose.mainClock.advanceTimeBy(PREVIEW_STEP_MS)
        assertEquals("Kurve nach Pause", top, curveTop())
    }

    @Test
    fun `Werkzeugleiste behaelt ihre Hoehe beim Wechsel der Auswahl`() {
        // Viloma I: Einatmen, „Halten“ (kurz) und Ausatmen; dazu zweistellige Nummern und 30 s.
        val state = show(PatternEditorState.new(prefix, template("Viloma I")))
        val height = toolbarHeight()
        state.pattern.phases.indices.forEach { index ->
            compose.runOnIdle { state.select(index) }
            compose.waitForIdle()
            assertEquals("Werkzeugleiste mit Phase ${index + 1} (${state.pattern.labelAt(index)})", height, toolbarHeight())
        }
        compose.runOnIdle {
            repeat(FreePattern.MAX_PHASES - state.pattern.phases.size) { state.add(FreePhaseType.HOLD) }
            state.setSeconds(state.selected, FreePattern.SECONDS_RANGE.last)
        }
        compose.waitForIdle()
        assertEquals("Werkzeugleiste mit Phase 12 und 30 s", height, toolbarHeight())
    }
}
