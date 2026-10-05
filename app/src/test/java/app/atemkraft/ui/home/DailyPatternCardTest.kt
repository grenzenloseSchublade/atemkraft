package app.atemkraft.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.atemkraft.R
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * „Muster des Tages“ auf der Startseite: Neu generieren im Abschnittskopf, Lesezeichen in der
 * Kartenecke, Karte startet die Übung. Die drei Ziele dürfen sich nicht gegenseitig auslösen,
 * und TalkBack muss Name, Rolle und Zustand des Lesezeichens lesen (A11Y-01, -04, KOMP-02).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w384dp-h832dp-xxhdpi")
class DailyPatternCardTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val daily = RandomPatternGenerator.forDate(LocalDate.of(2026, 10, 3))
    private var saved by mutableStateOf(false)
    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        compose.setContent {
            AtemkraftTheme {
                HomeScreen(
                    exercisesByFamily = emptyList(),
                    programs = emptyList(),
                    daily = daily,
                    dailySaved = saved,
                    onRegenerateDaily = { events += "neu" },
                    onDailySavedChange = {
                        saved = it
                        events += "gespeichert=$it"
                    },
                    onSelect = { events += "start:$it" },
                    onOpenSettings = {},
                )
            }
        }
    }

    @Test
    fun `Lesezeichen schaltet um, ohne die Karte zu starten, und meldet Rolle und Zustand`() {
        val bookmark = compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_save))
        bookmark
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, app.getString(R.string.state_not_saved)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .assertIsOff()

        bookmark.performClick()
        bookmark
            .assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, app.getString(R.string.state_saved)))
        bookmark.performClick()

        assertEquals(listOf("gespeichert=true", "gespeichert=false"), events)
    }

    @Test
    fun `Lesezeichen liegt mit voller Tippflaeche in der Kartenecke`() {
        // Die Klick-Semantik sitzt im sichtbaren Kreis; die 48-dp-Reservierung ist der
        // Layout-Knoten drumherum (minimumInteractiveComponentSize), zentriert um denselben Punkt.
        val node = compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_save)).fetchSemanticsNode()
        val c = node.boundsInRoot.center
        val half = Offset(node.layoutInfo.width / 2f, node.layoutInfo.height / 2f)
        val bookmark = Rect(c - half, c + half)
        val card = compose.onNodeWithText(daily.exercise.name, substring = true).fetchSemanticsNode().boundsInRoot
        val px48 = 48 * compose.density.density
        assertTrue("Tippfläche ${bookmark.width} × ${bookmark.height} px", bookmark.width >= px48 - 0.5f && bookmark.height >= px48 - 0.5f)
        // Overlay: oben rechts bündig in der Karte, ragt nicht heraus (A11Y-05, KOMP-02).
        assertEquals(card.top, bookmark.top, 1f)
        assertEquals(card.right, bookmark.right, 1f)
        assertTrue(bookmark.bottom <= card.bottom + 0.5f)
    }

    @Test
    fun `Karte startet das Muster, Neu generieren sitzt im Abschnittskopf`() {
        compose.onNodeWithText(daily.exercise.name, substring = true).performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_regenerate))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        compose.onNode(isHeading() and hasText(app.getString(R.string.home_daily_pattern))).assertExists()

        assertEquals(listOf("start:${daily.exercise.id}", "neu"), events)
    }

    @Test
    fun `Dauer haelt Zahl und Einheit mit geschuetztem Leerzeichen zusammen`() {
        val duration = app.getString(R.string.duration_approx, daily.exercise.defaultMinutes())
        assertEquals("ca. ${daily.exercise.defaultMinutes()} min", duration)
        compose.onNodeWithText(RandomPatternGenerator.HINT_SEPARATOR + duration, substring = true).assertExists()
    }
}
