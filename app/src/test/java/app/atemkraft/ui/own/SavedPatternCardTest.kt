package app.atemkraft.ui.own

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Karten eigener Muster mit freier Folge in „Meine Muster“: Die Zeile zeigt die Folge selbst, nicht
 * ihre Vier-Felder-Zusammenfassung (die erfände Phasen), und ein intensives Muster trägt
 * „intensiv“ wie auf der Detailseite.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class SavedPatternCardTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()

    /** So wie das Repository eine Zeile mit freier Folge liest. */
    private fun saved(id: Long, name: String, free: FreePattern) = SavedPattern(
        id = id,
        exercise = RandomPatternGenerator.exerciseFrom(free, "muster-$id", name),
        spec = free.columnSpec(),
        origin = PatternOrigin.CUSTOM,
        pattern = free,
        freeSequence = true,
    )

    @Test
    fun `freie Folge zeigt ihre Phasen und intensive Muster sind gekennzeichnet`() {
        val wechsel = saved(1, "Wechsel", FreePatternTemplates.all.single { it.label == "Wechsel" }.pattern)
        val intense = saved(2, "Lang halten", FreePattern.parse("in:4,hold:20,out:6")!!)
        compose.setContent {
            AtemkraftTheme {
                OwnPatternsScreen(savedPatterns = listOf(wechsel, intense), onNewPattern = {}, onSelect = {}, onDeletePattern = {}, onOpenSettings = {})
            }
        }
        assertEquals(wechsel.pattern.hint(), wechsel.patternLine)
        compose.onNodeWithText(wechsel.pattern.hint(), substring = true).assertExists()
        // Die Zusammenfassung „Einatmen 7 s · Halten 2 s · Ausatmen 11 s“ gibt es als Folge nicht.
        compose.onNodeWithText(RandomPatternGenerator.hintFor(wechsel.spec) + DOT_SEPARATOR, substring = true).assertDoesNotExist()
        compose.onNodeWithText(app.getString(R.string.detail_intense)).assertExists()
    }
}
