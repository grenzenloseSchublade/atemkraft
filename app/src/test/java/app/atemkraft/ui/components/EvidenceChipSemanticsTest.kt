package app.atemkraft.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import app.atemkraft.R
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.ui.TestDetail
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.home.ExerciseCard
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * A11Y/TEXT-07: Die Detailseite liest die Studienlage als „Studienlage: …“, vor dem
 * Vorsichts-Etikett. Die Karte in der Liste trägt keine Studienlage, nur Vorsicht und die
 * persönliche Markierung „Tut mir gut“.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class EvidenceChipSemanticsTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val wimHof = BuiltInExercises.all.first { it.id == "wim-hof" }
    private val evidence = app.getString(R.string.cd_evidence, app.getString(R.string.tag_studied))

    @Test
    fun `Detailseite liest Studienlage vor Vorsicht`() {
        compose.setContent {
            AtemkraftTheme {
                TestDetail(
                    exercise = wimHof,
                )
            }
        }

        val texts = compose.onAllNodes(hasText(evidence).or(hasText(app.getString(R.string.tag_caution))))
            .fetchSemanticsNodes().flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.map { it.text }
        assertEquals(listOf(evidence, app.getString(R.string.tag_caution)), texts)
    }

    @Test
    fun `Karte zeigt keine Studienlage, aber Vorsicht und Markierung`() {
        var marked by mutableStateOf(true)
        compose.setContent { AtemkraftTheme { ExerciseCard(wimHof, marked = marked) {} } }

        val card = compose.onNode(hasText("Wim Hof")).fetchSemanticsNode().config
        val texts = card.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        assertEquals(listOf("Wim Hof", app.getString(R.string.tag_caution)), texts.take(2))
        assertFalse(evidence in texts)
        assertTrue(app.getString(R.string.cd_personal_mark) in card.getOrNull(SemanticsProperties.ContentDescription).orEmpty())

        // Ohne Markierung kein Atemkreis.
        marked = false
        val unmarked = compose.onNode(hasText("Wim Hof")).fetchSemanticsNode().config
        assertFalse(app.getString(R.string.cd_personal_mark) in unmarked.getOrNull(SemanticsProperties.ContentDescription).orEmpty())
    }
}
