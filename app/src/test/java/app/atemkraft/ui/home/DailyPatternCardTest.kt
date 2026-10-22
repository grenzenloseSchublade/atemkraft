package app.atemkraft.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.atemkraft.R
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
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
 * Kartenecke, Karte startet die Übung, Link zum Editor darunter. Die Ziele dürfen sich nicht
 * gegenseitig auslösen, und TalkBack liest Rolle und Zustand des Lesezeichens (A11Y-01, -04,
 * KOMP-02). Tippfläche und Lage prüfen die Screenshot-Gates (`01_atmen`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
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
                    onOpenPatternEditor = { events += "editor" },
                )
            }
        }
    }

    @Test
    fun `Lesezeichen schaltet um, ohne die Karte zu starten, und meldet Rolle und Zustand`() {
        val bookmark = compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_save))
        bookmark
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsOff()
            .performClick()
        bookmark.assertIsOn().performClick()
        assertEquals(listOf("gespeichert=true", "gespeichert=false"), events)
    }

    /** Die drei Ziele lösen sich nicht gegenseitig aus; in der Karte gibt es nur Start und Lesezeichen. */
    @Test
    fun `Karte, Neu generieren und Editor-Link loesen je genau ihre Aktion aus`() {
        val card = compose.onNodeWithText(daily.exercise.name, substring = true).fetchSemanticsNode().boundsInRoot
        val inCard = compose.onAllNodes(hasClickAction(), useUnmergedTree = true).fetchSemanticsNodes()
            .map { it.boundsInRoot }
            .filter { it.top >= card.top - 0.5f && it.bottom <= card.bottom + 0.5f && it.left >= card.left - 0.5f && it.right <= card.right + 0.5f }
        assertEquals(2, inCard.size)

        compose.onNodeWithText(daily.exercise.name, substring = true).performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_regenerate))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        compose.onNodeWithText(app.getString(R.string.home_own_pattern_link))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        assertEquals(listOf("start:${daily.exercise.id}", "neu", "editor"), events)
    }
}
