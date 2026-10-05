package app.atemkraft.ui.situations

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.atemkraft.R
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.Situations
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Situationen-Tab als Befindens-Übersicht (MUSTER-03): Abschnitte starten zu, die ganze
 * Kopfzeile klappt auf, und TalkBack hört Überschrift, Rolle und Zustand (A11Y-01, -04).
 * Der Screenshot-Test zeigt nur, wie es aussieht – hier wird das Verhalten festgehalten.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w384dp-h832dp-xxhdpi")
class SituationsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val first = Situations.all.first()
    private val firstExercise = first.exerciseIds.firstNotNullOf { id -> BuiltInExercises.all.firstOrNull { it.id == id } }
    private val started = mutableListOf<String>()

    @Before
    fun setUp() {
        compose.setContent {
            AtemkraftTheme {
                SituationsScreen(
                    recommendations = Situations.all,
                    savedPatterns = emptyList(),
                    resolve = { id -> BuiltInExercises.all.firstOrNull { it.id == id } },
                    onSelect = { started += it },
                    onDeleteSaved = {},
                )
            }
        }
    }

    private fun header() = compose.onNode(hasText(first.title) and hasClickAction())

    private fun stateIs(resId: Int) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, app.getString(resId))

    @Test
    fun `Abschnitte starten zu und zeigen Titel und Begründung, aber noch keine Übung`() {
        compose.onNodeWithText(first.rationale).assertExists()
        compose.onNodeWithText(firstExercise.name).assertDoesNotExist()
        header().assert(stateIs(R.string.state_collapsed))
    }

    @Test
    fun `Kopfzeile ist Überschrift und Schaltfläche und klappt per Tipp auf und wieder zu`() {
        header()
            .assert(isHeading())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        header().assert(stateIs(R.string.state_expanded))
        compose.onNodeWithText(firstExercise.name).assertExists()

        header().performClick()
        header().assert(stateIs(R.string.state_collapsed))
        compose.onNodeWithText(firstExercise.name).assertDoesNotExist()
    }

    @Test
    fun `Übungskarte im aufgeklappten Abschnitt startet die Übung, die Kopfzeile nicht`() {
        header().performClick()
        assertEquals(emptyList<String>(), started)
        compose.onNodeWithText(firstExercise.name).performClick()
        assertEquals(listOf(firstExercise.id), started)
    }
}
