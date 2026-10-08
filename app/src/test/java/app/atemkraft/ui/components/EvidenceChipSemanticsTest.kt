package app.atemkraft.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import app.atemkraft.R
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.ui.home.ExerciseCard
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * A11Y/TEXT-07: TalkBack liest die Studienlage als „Studienlage: …“, in der Reihenfolge der
 * sichtbaren Chips (Titel → Studienlage → Vorsicht → Teaser), innerhalb der zusammengeführten Karte.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w384dp-h832dp-xxhdpi")
class EvidenceChipSemanticsTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()

    @Test
    fun `Karte liest Studienlage vor Vorsicht`() {
        val wimHof = BuiltInExercises.all.first { it.id == "wim-hof" }
        compose.setContent { AtemkraftTheme { ExerciseCard(wimHof) {} } }

        val evidence = app.getString(R.string.cd_evidence, app.getString(R.string.tag_studied))
        val texts = compose.onNode(hasText(evidence)).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        assertEquals(listOf("Wim Hof", evidence, app.getString(R.string.tag_caution)), texts.take(3))
    }
}
