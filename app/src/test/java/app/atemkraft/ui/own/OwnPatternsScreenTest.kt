package app.atemkraft.ui.own

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import app.atemkraft.data.local.columnSpec
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.PatternOrigin
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
 * Tab „Eigene“: Kopf wie die anderen Tabs, „Neues Muster“ als Knopf, Leerzustand mit Satz und
 * Knopf, Löschen zweistufig (MUSTER-05), Karten freier Folgen ehrlich mit „intensiv“.
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

    private var patterns by mutableStateOf(savedAll)
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
                    savedPatterns = patterns.take(patternCount).filter { it.id !in deletedIds },
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

    /** Kopf wie die anderen Tabs (Überschrift, Zahnrad); „Neues Muster“ und Karte lösen je ihre Aktion aus. */
    @Test
    fun `Kopf ist Ueberschrift, Zahnrad, Neues Muster und Karte loesen je ihre Aktion aus`() {
        compose.onNodeWithText(str(R.string.own_title)).assert(isHeading())
        compose.onNodeWithContentDescription(str(R.string.settings_title)).performClick()
        assertEquals(1, settingsTaps)
        newPatternButton()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        assertEquals(1, newPatternTaps)
        val first = savedAll.first()
        compose.onNodeWithText(first.exercise.name).performClick()
        assertEquals(listOf(first.exercise.id), selected)
    }

    @Test
    fun `ohne Muster stehen ein kurzer Satz und der Knopf da`() {
        patternCount = 0
        compose.onNodeWithText(str(R.string.own_empty)).assertExists()
        newPatternButton().performClick()
        assertEquals(1, newPatternTaps)
    }

    /** Schutz gegen Datenverlust: Die Schärfung hängt am Muster, nicht an der Position. */
    @Test
    fun `Löschen ist zweistufig und bleibt beim Muster, nicht bei der Position`() {
        patternCount = 2
        val (first, second) = savedAll.take(2)
        deleteButton(first).performClick()
        assertEquals(emptySet<Long>(), deletedIds)
        compose.onNodeWithContentDescription(app.getString(R.string.cd_delete_confirm, first.exercise.name)).performClick()
        assertEquals(setOf(first.id), deletedIds)
        assertEquals(emptyList<String>(), selected)
        // Das nachrückende Muster ist nicht scharf: ein Tipp löscht es nicht.
        patternCards().assertCountEquals(1)
        deleteButton(second).performClick()
        assertEquals(setOf(first.id), deletedIds)
    }

    /**
     * Karten mit freier Folge zeigen die Folge selbst, nicht ihre Vier-Felder-Zusammenfassung (die
     * erfände Phasen), und ein intensives Muster trägt „intensiv“ wie auf der Detailseite.
     */
    @Test
    fun `freie Folge zeigt ihre Phasen und intensive Muster sind gekennzeichnet`() {
        fun saved(id: Long, name: String, free: FreePattern) = SavedPattern(
            id = id,
            exercise = RandomPatternGenerator.exerciseFrom(free, "muster-$id", name),
            spec = free.columnSpec(),
            origin = PatternOrigin.CUSTOM,
            pattern = free,
            freeSequence = true,
        )
        val wechsel = saved(1, "Wechsel", FreePatternTemplates.all.single { it.label == "Wechsel" }.pattern)
        val intense = saved(2, "Lang halten", FreePattern.parse("in:4,hold:20,out:6")!!)
        patterns = listOf(wechsel, intense)
        assertEquals(wechsel.pattern.hint(), wechsel.patternLine)
        compose.onNodeWithText(wechsel.pattern.hint(), substring = true).assertExists()
        // Die Zusammenfassung „Einatmen 7 s · Halten 2 s · Ausatmen 11 s“ gibt es als Folge nicht.
        compose.onNodeWithText(RandomPatternGenerator.hintFor(wechsel.spec) + DOT_SEPARATOR, substring = true).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.detail_intense)).assertExists()
    }
}
