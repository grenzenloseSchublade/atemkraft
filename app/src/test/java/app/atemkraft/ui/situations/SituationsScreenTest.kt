package app.atemkraft.ui.situations

import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import app.atemkraft.R
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.SavedPattern
import app.atemkraft.data.Situations
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.Situation
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

/**
 * Situationen-Tab als Befindens-Übersicht (MUSTER-03): Abschnitte starten zu, die ganze
 * Kopfzeile klappt auf, und TalkBack hört Überschrift, Rolle und Zustand (A11Y-01, -04).
 * Dazu die Befindens-Suche (MUSTER-09): Lupe öffnet ein Feld mit Fokus, Treffer stehen
 * aufgeklappt allein da, „Nichts gefunden“ bietet „Alle zeigen“, X und System-Zurück schließen,
 * und die Tastatur lernt nichts (SEC-PRIV-03).
 * Der Screenshot-Test zeigt nur, wie es aussieht – hier wird das Verhalten festgehalten.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w384dp-h832dp-xxhdpi")
class SituationsScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()
    private val first = Situations.all.first()
    private val firstExercise = first.exerciseIds.firstNotNullOf { id -> BuiltInExercises.all.firstOrNull { it.id == id } }
    private val started = mutableListOf<String>()
    private val saved = RandomPatternGenerator.forSeed(11).let { SavedPattern(id = 1L, exercise = it.exercise, spec = it.spec) }
    private val crash = Situations.all.single { it.situation == Situation.CRASH }

    @Before
    fun setUp() {
        compose.setContent {
            AtemkraftTheme {
                SituationsScreen(
                    recommendations = Situations.all,
                    savedPatterns = listOf(saved),
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

    private fun str(resId: Int) = app.getString(resId)

    private fun openSearch() = compose.onNodeWithContentDescription(str(R.string.cd_search_open)).performClick()

    private fun field() = compose.onNode(hasSetTextAction())

    @Test
    fun `Lupe öffnet das Suchfeld mit Fokus anstelle des Untertitels`() {
        compose.onNodeWithText(str(R.string.situations_subtitle)).assertExists()
        openSearch()
        field().assertIsFocused()
        compose.onNodeWithText(str(R.string.situations_subtitle)).assertDoesNotExist()
        compose.onNodeWithContentDescription(str(R.string.cd_search_open)).assertDoesNotExist()
        // Leeres Feld: Übersicht bleibt, auch „Meine Muster“.
        compose.onNodeWithText(str(R.string.situations_my_patterns)).assertExists()
    }

    @Test
    fun `Treffer stehen aufgeklappt allein da, Meine Muster treten zurück, die Zahl wird angesagt`() {
        openSearch()
        field().performTextInput("müde")
        compose.onNodeWithText(crash.title).assertExists()
        crash.exerciseIds.mapNotNull { id -> BuiltInExercises.all.firstOrNull { it.id == id } }
            .forEach { compose.onNodeWithText(it.name).assertExists() }
        Situations.all.filter { it != crash }.forEach { compose.onNodeWithText(it.title).assertDoesNotExist() }
        compose.onNodeWithText(str(R.string.situations_my_patterns)).assertDoesNotExist()
        val count = app.resources.getQuantityString(R.plurals.situations_search_count, 1, 1)
        compose.onNodeWithText(count)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun `ohne Treffer heißt es Nichts gefunden und Alle zeigen führt zur ganzen Übersicht zurück`() {
        openSearch()
        field().performTextInput("Asthma")
        Situations.all.forEach { compose.onNodeWithText(it.title).assertDoesNotExist() }
        compose.onNodeWithText(str(R.string.situations_search_none)).assertExists()
        compose.onNodeWithText(str(R.string.action_show_all)).performClick()
        field().assertDoesNotExist()
        Situations.all.forEach { compose.onNodeWithText(it.title).assertExists() }
    }

    @Test
    fun `X leert zuerst die Eingabe und schließt dann die Suche`() {
        openSearch()
        field().performTextInput("müde")
        compose.onNodeWithContentDescription(str(R.string.cd_search_clear)).performClick()
        field().assertExists()
        compose.onNodeWithText(first.title).assertExists()
        compose.onNodeWithContentDescription(str(R.string.cd_search_close)).performClick()
        field().assertDoesNotExist()
        compose.onNodeWithText(str(R.string.situations_subtitle)).assertExists()
    }

    @Test
    fun `System-Zurück schließt zuerst die Suche`() {
        openSearch()
        field().performTextInput("müde")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        field().assertDoesNotExist()
        Situations.all.forEach { compose.onNodeWithText(it.title).assertExists() }
        assertTrue(!compose.activity.isFinishing)
    }

    @Test
    fun `Tastatur ohne Autokorrektur und ohne Lernen, Aktion Suchen`() {
        openSearch()
        val info = EditorInfo()
        compose.runOnIdle { composeView(compose.activity.window.decorView)!!.onCreateInputConnection(info) }
        assertEquals(EditorInfo.IME_ACTION_SEARCH, info.imeOptions and EditorInfo.IME_MASK_ACTION)
        assertTrue("IME_FLAG_NO_PERSONALIZED_LEARNING fehlt", info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0)
        assertEquals(0, info.inputType and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
        field().performImeAction()
    }

    /** Die View, die Compose die Texteingabe liefert (AndroidComposeView). */
    private fun composeView(root: View): View? = when {
        root.javaClass.simpleName == "AndroidComposeView" -> root
        root is ViewGroup -> (0 until root.childCount).firstNotNullOfOrNull { composeView(root.getChildAt(it)) }
        else -> null
    }
}
