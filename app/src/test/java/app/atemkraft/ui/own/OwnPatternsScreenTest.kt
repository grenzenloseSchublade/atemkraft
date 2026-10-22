package app.atemkraft.ui.own

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.atemkraft.R
import app.atemkraft.data.SavedPattern
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

/**
 * Tab „Eigene“: Kopf wie die anderen Tabs, „Neues Muster“ als Knopf, „Meine Muster“ immer offen
 * (kein Aufklappen mehr), Leerzustand mit Satz und Knopf, Löschen zweistufig (MUSTER-05).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class OwnPatternsScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()
    private val savedAll = listOf(11L, 42L, 7L, 3L).mapIndexed { i, seed ->
        RandomPatternGenerator.forSeed(seed).let { SavedPattern(id = i + 1L, exercise = it.exercise, spec = it.spec) }
    }

    private var patternCount by mutableIntStateOf(4)

    // Gelöschte Muster verschwinden wie in MainActivity aus der Liste (Room → Flow).
    private var deletedIds by mutableStateOf(emptySet<Long>())
    private val selected = mutableListOf<String>()
    private var newPatternTaps = 0
    private var settingsTaps = 0

    @Before
    fun setUp() {
        compose.setContent {
            AtemkraftTheme {
                OwnPatternsScreen(
                    savedPatterns = savedAll.take(patternCount).filter { it.id !in deletedIds },
                    onNewPattern = { newPatternTaps++ },
                    onSelect = { selected += it },
                    onDeletePattern = { deletedIds = deletedIds + it },
                    onOpenSettings = { settingsTaps++ },
                )
            }
        }
    }

    private fun str(resId: Int) = app.getString(resId)

    private fun newPatternButton() = compose.onNode(hasText(str(R.string.own_new_pattern)) and hasClickAction())

    /** Löschen-Knopf von [pattern]: Name in der Beschreibung (A11Y-06). */
    private fun deleteButton(pattern: SavedPattern) = compose.onNodeWithContentDescription(app.getString(R.string.cd_pattern_delete, pattern.exercise.name))

    /** Ein Löschen-Knopf je sichtbarer Karte. */
    private fun patternCards() = compose.onAllNodes(
        SemanticsMatcher("Löschen-Knopf eines Musters") { node ->
            val cds = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
            savedAll.any { app.getString(R.string.cd_pattern_delete, it.exercise.name) in cds }
        },
    )

    @Test
    fun `Kopf mit Titel, Untertitel und Zahnrad wie die anderen Tabs`() {
        compose.onNodeWithText(str(R.string.own_title)).assert(isHeading())
        compose.onNodeWithText(str(R.string.own_subtitle)).assertExists()
        compose.onNodeWithContentDescription(str(R.string.settings_title)).performClick()
        assertEquals(1, settingsTaps)
    }

    @Test
    fun `Neues Muster ist ein Knopf und meldet den Tipp`() {
        newPatternButton()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        assertEquals(1, newPatternTaps)
    }

    @Test
    fun `Meine Muster stehen immer offen, ohne Aufklappen, alle Karten da`() {
        compose.onNodeWithText(str(R.string.situations_my_patterns))
            .assert(isHeading())
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        compose.onNode(hasText(str(R.string.situations_my_patterns)) and hasClickAction()).assertDoesNotExist()
        patternCards().assertCountEquals(4)
        compose.onNodeWithText(str(R.string.own_empty)).assertDoesNotExist()
    }

    @Test
    fun `Tipp auf eine Karte öffnet das Muster`() {
        val first = savedAll.first()
        compose.onNodeWithText(first.exercise.name).performClick()
        assertEquals(listOf(first.exercise.id), selected)
    }

    @Test
    fun `ohne Muster stehen ein kurzer Satz und der Knopf da, kein Abschnittskopf`() {
        patternCount = 0
        compose.onNodeWithText(str(R.string.own_empty)).assertExists()
        compose.onNodeWithText(str(R.string.situations_my_patterns)).assertDoesNotExist()
        patternCards().assertCountEquals(0)
        newPatternButton().performClick()
        assertEquals(1, newPatternTaps)
    }

    @Test
    fun `Löschen ist zweistufig, angesagt und bleibt beim Muster, nicht bei der Position`() {
        patternCount = 2
        val (first, second) = savedAll.take(2)
        deleteButton(first).performClick()
        assertEquals(emptySet<Long>(), deletedIds)
        compose.onNodeWithContentDescription(app.getString(R.string.cd_delete_confirm, first.exercise.name))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .performClick()
        assertEquals(setOf(first.id), deletedIds)
        assertEquals(emptyList<String>(), selected)
        // Das nachrückende Muster ist nicht scharf: ein Tipp löscht es nicht.
        patternCards().assertCountEquals(1)
        deleteButton(second).performClick()
        assertEquals(setOf(first.id), deletedIds)
    }
}
