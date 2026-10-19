package app.atemkraft.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import app.atemkraft.R
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
 * A11Y-06/-01: Was TalkBack statt der sichtbaren Kurzform hört – Dauer ausgeschrieben
 * („6 Minuten 5 Sekunden“ statt „6:05“), Stepper-Wert mit Einheit („Einatmen: 4 Sekunden“ statt
 * „Einatmen (s): 4“) – und dass Kartentitel Überschriften sind.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class SpokenTextTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()

    /** Je Dauer die Ansage; ein setContent pro Test. */
    private fun spoken(vararg durationsMs: Long): List<String> {
        val result = mutableListOf<String>()
        compose.setContent { durationsMs.forEach { result += spokenDuration(it) } }
        compose.waitForIdle()
        return result
    }

    @Test
    fun `Dauer wird mit Einheiten ausgeschrieben, ohne Nullteil`() {
        assertEquals(
            listOf("6 Minuten 5 Sekunden", "10 Minuten", "1 Minute 1 Sekunde", "45 Sekunden", "0 Sekunden"),
            spoken(365_000L, 600_000L, 61_000L, 45_000L, 0L),
        )
    }

    private fun show(content: @Composable () -> Unit) {
        compose.setContent { AtemkraftTheme { content() } }
    }

    @Test
    fun `Stepper sagt Name und Einheit ohne Klammer`() {
        show {
            Stepper(
                label = app.getString(R.string.adjust_inhale),
                value = 4,
                range = 1..10,
                onChange = {},
                spokenLabel = app.getString(R.string.phase_inhale),
                valueUnit = R.plurals.cd_duration_seconds,
            )
        }
        compose.onNodeWithContentDescription("Einatmen: 4 Sekunden").assertExists()
        compose.onNodeWithContentDescription("Einatmen, ${app.getString(R.string.adjust_increase)}").assertExists()
        // Sichtbar bleibt das kurze Label mit Einheit.
        compose.onNodeWithText(app.getString(R.string.adjust_inhale)).assertExists()
    }

    @Test
    fun `Kartentitel ist eine Ueberschrift`() {
        show { CardTitle("Ton") }
        compose.onNodeWithText("Ton").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }
}
